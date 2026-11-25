# Sightline

Sightline is a location-based AR camera and map app for discovering nearby places by category.

## Language

**Place**:
A named point of interest with geographic coordinates and a single category.
_Avoid_: POI, venue, spot

**Place Category**:
One of the thirteen tourist and exploration groupings that a user can toggle on or off to fetch and filter nearby places.
_Avoid_: Tag, filter, group

**Search Radius**:
The maximum distance from the active location within which nearby places are fetched for each active place category.
_Avoid_: Range, distance limit

**User Location**:
The live geographic fix reported by the device's location services when live tracking is enabled.
_Avoid_: Device location, GPS fix

**Custom Location**:
A user-selected geographic coordinate and address gained from an address search, used as the origin for place queries when live user location is disabled.
_Avoid_: Manual location, pinned location, fake location

**Active Location**:
The geographic coordinate currently used as the origin for place queries and distance calculations: the user location when live tracking is enabled, or the custom location otherwise.
_Avoid_: Current location, effective location, search origin

**AR Marker**:
A screen-space card overlaid on the camera view showing a place's category icon, name, and distance from the active location.
_Avoid_: Pin, callout, annotation

**Marker Slot**:
A vertical row and page assignment calculated from bearing overlap so nearby AR markers stack cleanly instead of colliding on screen.
_Avoid_: Bucket, lane

**Screen Rotation**:
The current interface orientation of the screen relative to the device's natural upright orientation, used to align the camera preview and AR marker projection when the device rotates between portrait and landscape.
_Avoid_: Device tilt, roll angle
