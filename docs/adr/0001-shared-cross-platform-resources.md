# Shared cross-platform resources

User-visible strings and Place Category icons must be identical on Android and iOS, so they live in one place: the `:core:resources` KMP module, managed by `moko-resources`. Android renders its generated `VectorDrawable`s and string resources; iOS renders its generated asset catalog and `Localizable.strings` inside a bundle the static `Shared` framework copies into the app. Domain types become user-visible resources in that module (`PlaceCategory.label`, `PlaceCategory.icon`, `RequestError.message`), so a category added on one platform cannot drift from the other.

## Considered Options

- **Duplicate `strings.xml` and Swift literals** - rejected: the two platforms already drifted (camera copy, OSM attribution) and nothing catches a missing translation.
- **Compose Multiplatform resources** - rejected: the iOS UI is SwiftUI, and the generated `Res` API is a Kotlin API, not a Swift one.
- **A homegrown codegen** - rejected: it would reimplement SVG to `VectorDrawable` conversion, asset catalogs, and string bundles for no gain.

## Consequences

- Resources are checked in as SVG, not Android XML. moko accepts no Android vector input; SVG is converted to a `VectorDrawable` on Android and a vector-preserving asset on iOS.
- `:shared` applies the moko plugin even though it holds no resources, because only a module that compiles an iOS framework can copy klib resource bundles into the app.
- The iOS app carries `Sightline.core:resources.bundle` at its root, copied by `:shared:copyFrameworkResourcesToApp` from the `Build Shared framework` pre-build phase. That task writes outside the Gradle project, so it is marked never up-to-date.
- Prebuilt XCFrameworks ship without resources: only the Xcode-driven `embedAndSignAppleFrameworkForXcode` path copies the bundle. No workflow here consumes a prebuilt XCFramework; if one appears, it needs the per-build-type `copyResourcesShared<BuildType>XCFrameworkToApp` task (the build-type-agnostic task is broken on Kotlin 2.4).
- Shared format strings use positioned placeholders only (`%1$d`, `%1$s`). moko rewrites `%s`/`%d` to `%@` on iOS, so `%.2f` silently loses its precision; format numbers before passing them.
- Android call sites import individual string accessors (`import com.trm.sightline.core.resources.places_count`) because moko 0.27 generates them as top-level extension properties.
- Kotlin-side mappings are the only place a `PlaceCategory` or `RequestError` is turned into copy. iOS reads `category.label.localized` and `error.localizedMessage`; Android reads `category.label.localized()` in Compose.
