# Research: Android -> iOS AR orientation remapping (ticket 09 prototype)

Status: complete (analytic derivation + throwaway prototype, Nov 2026)

Branch: `prototype/09-ar-orientation`
Prototype: `prototype/ar-orientation/`

## TL;DR

The exact remapping from iOS `CMDeviceMotion.attitude` (`.xMagneticNorthZVertical`) to the
`(pitch, azimuth, roll)` triple that Android's `OrientationManager` feeds into the shared
`Math3D.getCamRotation` is, using the attitude rotation matrix (primary, gimbal-lock free):

```swift
let m = motion.attitude.rotationMatrix   // CMRotationMatrix, maps reference -> device
let azimuth = atan2(-m.m12, m.m11)       // -> Android getOrientation values[0]
let pitch   = asin(-m.m13)               // -> Android getOrientation values[1]
let roll    = atan2(-m.m23, m.m33)       // -> Android getOrientation values[2]
```

`pitch`, `azimuth`, `roll` above are then passed straight into the unchanged shared
`Math3D.getCamRotation`, which does `camRot.x = -pitch`, `camRot.y = azimuth + PI`,
`camRot.z = 0`, and `screenRot = -roll` (iOS portrait == Android `Surface.ROTATION_0`).

The equivalent Euler-angle form (convenient but singular at pitch = +/-pi/2, see caveat) is:

```swift
let y = motion.attitude.yaw
let p = motion.attitude.pitch
let r = motion.attitude.roll
let azimuth = atan2(sin(y)*cos(p),
                    cos(y)*cos(r) - sin(y)*sin(p)*sin(r))
let pitch   = asin(-(cos(y)*sin(r) + sin(y)*sin(p)*cos(r)))
let roll    = atan2(cos(y)*sin(p)*cos(r) - sin(y)*sin(r),
                    cos(p)*cos(r))
```

This derivation is analytic and was cross-checked numerically against three physical
poses (flat, upright portrait, 30-degree tilt).
On-device verification with a real camera + magnetometer is the remaining human step.

## What this ticket produces

A throwaway prototype that renders a live `AVCaptureSession` feed in an `MTKView` and drives a
test marker from `CMMotionManager` attitude, plus this recorded finding.
The finding is the deliverable: ticket 24 (`24-ios-ar-camera.md`) commits to this remapping and
must not re-derive it.
The prototype is on branch `prototype/09-ar-orientation` and does not merge into the integration branch.

## Android path (what we match)

`core/ar/.../orientation/OrientationManager.kt`:

1. `Sensor.TYPE_ROTATION_VECTOR` -> `SensorManager.getRotationMatrixFromVector(rotationM, ...)`.
2. `SensorManager.remapCoordinateSystem(rotationM, AXIS_Y, AXIS_MINUS_X, remapped)`.
3. `SensorManager.getOrientation(remapped, out)` -> `out[0]=azimuth, out[1]=pitch, out[2]=roll`.
4. Low-pass smooth each angle (`SMOOTH_FACTOR = 0.12`, threshold `CIRCLE/20 ~= 18 deg`, with wrap handling).
5. `Math3D.getCamRotation`: `outCamRot.x = -pitch`, `outCamRot.y = azimuth + PI`, `outCamRot.z = 0`;
   screen Z from `Surface.ROTATION_*` + roll (`ROTATION_0 -> -roll`).

## iOS path

`CMMotionManager.startDeviceMotionUpdates(using: .xMagneticNorthZVertical)` ->
`motion.attitude.pitch/roll/yaw` (radians, -pi..pi).
Core Motion is already sensor-fused (gyro + accelerometer + magnetometer), so the Android
accelerometer+magnetometer fallback has no iOS counterpart and is simply omitted.

## Conventions established (with sources)

These are the facts the derivation depends on, each confirmed against primary or near-primary sources.

- Android world frame from `getRotationMatrix` is right-handed ENU: X = magnetic east, Y = magnetic north,
  Z = up (the sky). Verified from the AOSP `getRotationMatrix` source (rows are `H = E x A`, `M = A x H`, `A = gravity`).
- Android device frame (natural portrait): X = right, Y = top, Z = out of screen toward the viewer.
- Android `getRotationMatrix` returns R mapping device -> world (`v_world = R * v_device`),
  i.e. R's columns are the device axes expressed in world coordinates. Verified against the
  documented azimuth behavior (flat, top = north -> azimuth 0).
- AOSP `remapCoordinateSystem(R, AXIS_Y, AXIS_MINUS_X)` computes `outR = R * Rz(-90 deg)`.
  Read directly from `remapCoordinateSystemImpl`: it performs `outR = inR * r` with `r` the signed
  permutation matrix whose columns are the new device axes.
  For `AXIS_Y, AXIS_MINUS_X` the result is a -90 deg rotation about the device Z axis.
- iOS reference frame `.xMagneticNorthZVertical` is right-handed: X = magnetic north, Y = west, Z = up.
  (Swift By Deya, "Attitude, Reference Frames, and Coordinate Conversion", Feb 2026, and right-handedness
  north x west = up.)
- iOS `CMAttitude.rotationMatrix` maps reference -> device (`v_device = R * v_reference`).
  Confirmed by Apple's WWDC 2010 session 423: `gravityDevice = multiplyMatrixAndVector(R, {0,0,-1})`.
- iOS Euler decomposition is `R = Rz(yaw) * Rx(pitch) * Ry(roll)` (ZXY order).
  Confirmed by the VPhone `CMAttitude` sample quaternion (matches `q_z * q_x * q_y` exactly) and an
  empirical rotation-order study (StackOverflow 40128906).
- iOS device axes: pitch about X (lateral/left-right), roll about Y (longitudinal/top-bottom),
  yaw about Z (screen normal). `yaw == 0` when the device X (right edge) points to magnetic north.

## Derivation

Android `getOrientation` on a matrix R (columns = device axes in world) returns:

```
azimuth = atan2(R01, R11)     // bearing of the device Y (top) axis
pitch   = asin(-R21)          // -elevation of the device Y (top) axis
roll    = atan2(R20, R22)     // rotation of the right/out edges about the top axis
```

where `Rij` is the i-th world component of the j-th device axis (R[2][1] = up component of top, etc.).

After `remapCoordinateSystem(..., AXIS_Y, AXIS_MINUS_X)`, `remapped = R * Rz(-90 deg)` has columns:

```
remapped = [ -u | r | o ]
```

i.e. the remapped "top" axis is the device right edge `r`, the remapped "X" axis is `-top`, and the
remapped "out" axis is unchanged.
Substituting into the `getOrientation` formulas gives the Android output in terms of the physical
device axes (in ENU, east/north/up):

```
azimuth = atan2(rE, rN)       // bearing of the device RIGHT edge
pitch   = asin(-rU)           // -elevation of the device RIGHT edge
roll    = atan2(-uU, oU)      // u = top, o = out-of-screen
```

So the Android "azimuth" (after this remap) is the compass bearing of the device's right edge, not
the top edge.
This is the deliberate result of the `AXIS_Y, AXIS_MINUS_X` remap (the AOSP-documented
`ROTATION_90` remap) used by Sightline in `Mode.COMPASS`.

Now express the device axes in ENU from the iOS rotation matrix.
`R_ios` maps reference -> device, so `R_ios^T` maps device -> reference; the rows of `R_ios` are the
device axes expressed in the iOS reference frame (north, west, up).
The iOS reference frame differs from ENU by `A = Rz(+90 deg)` (`v_ENU = A * v_iosRef`), so:

```
r = A * row0(R_ios)   ->  r = (-m12,  m11,  m13)
u = A * row1(R_ios)   ->  u = (-m22,  m21,  m23)
o = A * row2(R_ios)   ->  o = (-m32,  m31,  m33)
```

Substituting `rE = -m12`, `rN = m11`, `rU = m13`, `uU = m23`, `oU = m33` yields the matrix form in
the TL;DR.
The Euler form follows by expanding `R_ios = Rz(yaw) Rx(pitch) Ry(roll)` into the same expressions.

Both forms were verified against three analytic test poses and reproduce the Android
`getOrientation` output exactly in each case.

### How iOS yaw maps to Android azimuth (and the +PI "0 = south" convention)

Both `yaw` (iOS) and `azimuth` (Android, post-remap) are compass angles with 0 = north and positive
toward east, but they are measured about different device axes:

- iOS `yaw` is the rotation about the device's screen-normal (Z) axis in the reference frame;
  `yaw == 0` when the device's right edge points north.
- Android `azimuth` (after `AXIS_Y, AXIS_MINUS_X`) is the bearing of the device's right edge.

They coincide only in the flat, level case (`pitch = roll = 0`), where `azimuth == yaw`.
In general they couple through pitch and roll (see the Euler form in the TL;DR).
After the remapping, `getCamRotation` adds `PI` (`outCamRot.y = azimuth + PI`) so that the camera
heading zero is south, not north; this `+PI` is applied in the shared `Math3D.getCamRotation` and is
therefore already handled - do not add it a second time on the iOS side.

## Screen rotation handling

Android reads `Surface.ROTATION_*` from `display.rotation` (`ARContextExtensions.phoneRotation`) and
folds it into `screenRot` only.
Sightline is used portrait, where `ROTATION_0` is natural portrait, so on iOS the fixed portrait
orientation is `ROTATION_0` and `screenRot = -roll`.
If the iOS app later supports landscape, map `UIInterfaceOrientation.landscapeLeft/Right` and
`portraitUpsideDown` to the corresponding `Surface.ROTATION_90/270/180` branches in `getCamRotation`.

## Smoothing

Reproduce the Android low-pass on the remapped `(azimuth, pitch, roll)` before `getCamRotation`:

```
SMOOTH_FACTOR  = 0.12
SMOOTH_THRESHOLD = 2*pi/20   // ~18 deg
```

with the same circular wrap handling (`OrientationManager.lowPass`) so azimuth does not jump across
the +/-pi boundary.
Core Motion already outputs fused, filtered attitude, so this smoothing is optional polish rather than
a correctness requirement; keep it to match the Android "feel".

## Caveats

- The Euler-angle form is singular at iOS `pitch = +/- pi/2` (the ZXY-convention gimbal lock), which is
  exactly the upright-portrait camera pose.
  Use the rotation-matrix form in the renderer; it has no singularity.
- The derivation is analytic.
  On-device position-matching (physical device with camera + magnetometer) is the remaining human step
  and was not performed here (the simulator has neither camera nor magnetometer).
- Only the orientation remapping is covered.
  Camera texture orientation (`AVCaptureConnection.videoRotationAngle`, mirroring) and center-crop are
  separate concerns already covered in `01-ar-camera-effect-ios.md` section 2/4.

## Prototype

`prototype/ar-orientation/` - a minimal xcodegen iOS app.
It renders the live `AVCaptureSession` feed into an `MTKView` (zero-copy `CVMetalTextureCache`) and
projects a fixed test marker through the full Android projection chain
(`getCamRotation` -> `getRelativeRotation` -> `convert3dTo2d`) using the remapping above.
The remapping lives in `Sources/OrientationRemapping.swift` and is the copy-paste target for ticket 24.

Build check: `xcodebuild` against an iOS simulator destination (compiles; no camera/magnetometer at runtime).

## Reference for ticket 24

Commit this note and the prototype under message `Add AR orientation remapping prototype and finding`.
Ticket 24 must use the matrix-form remapping verbatim and must not re-derive it.
