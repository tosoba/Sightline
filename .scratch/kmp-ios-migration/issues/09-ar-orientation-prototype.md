# 09: AR orientation remapping prototype

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`

**What to build:** A throwaway Metal + Core Motion spike that confirms whether the Android rotation-vector orientation path maps correctly to CMMotionManager's `.xMagneticNorthZVertical` attitude for aligning AR markers with the camera. It produces a recorded finding (the correct remapping, or the exact axis/sign adjustment needed) that the full Metal port commits to, de-risking the top parity risk before any renderer work.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] A minimal Metal view renders the camera feed plus a test marker using CMMotionManager attitude.
- [ ] The marker's screen position matches the equivalent Android behavior under the same device orientation, or the exact axis/sign remapping is documented.
- [ ] The finding is recorded on a prototype branch and referenced from ticket 24, so the full port does not re-derive it.
