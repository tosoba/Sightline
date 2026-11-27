import Shared
import SwiftUI

extension StringResource {
    /// The shared string in the current locale.
    var localized: String {
        desc().localized()
    }

    /// The shared string with its positional placeholders (`%1$d`, `%1$s`) filled in.
    func localized(_ arguments: [CVarArg]) -> String {
        String(format: localized, locale: .current, arguments: arguments)
    }
}

extension Shared.ImageResource {
    /// The icon rendered in template mode so SwiftUI tints it with the foreground style.
    var templateImage: UIImage? {
        toUIImage()?.withRenderingMode(.alwaysTemplate)
    }
}

extension RequestError {
    /// The shared copy for this error, mapped in the `:core:resources` module.
    var localizedMessage: String {
        RequestErrorResourcesKt.message(self).localized
    }
}

extension Text {
    init(_ keyPath: KeyPath<MR.strings, StringResource>) {
        self.init(MR.strings()[keyPath: keyPath].localized)
    }

    init(_ keyPath: KeyPath<MR.strings, StringResource>, _ arguments: CVarArg...) {
        self.init(MR.strings()[keyPath: keyPath].localized(arguments))
    }
}

extension Image {
    init(resource keyPath: KeyPath<MR.images, Shared.ImageResource>) {
        self.init(uiImage: MR.images()[keyPath: keyPath].templateImage ?? UIImage())
    }

    init(_ resource: Shared.ImageResource) {
        self.init(uiImage: resource.templateImage ?? UIImage())
    }
}
