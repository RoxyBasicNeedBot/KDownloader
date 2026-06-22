import SwiftUI

public struct KDownloaderTheme {
    public var accentColor: Color
    public var rowBackgroundColor: Color
    public var cornerRadius: CGFloat
    public var shadowRadius: CGFloat
    
    public init(
        accentColor: Color = .blue,
        #if canImport(UIKit)
        rowBackgroundColor: Color = Color(UIColor.secondarySystemBackground),
        #else
        rowBackgroundColor: Color = Color(NSColor.controlBackgroundColor),
        #endif
        cornerRadius: CGFloat = 12.0,
        shadowRadius: CGFloat = 2.0
    ) {
        self.accentColor = accentColor
        self.rowBackgroundColor = rowBackgroundColor
        self.cornerRadius = cornerRadius
        self.shadowRadius = shadowRadius
    }
}

public struct KDownloaderThemeKey: EnvironmentKey {
    public static let defaultValue = KDownloaderTheme()
}

public extension EnvironmentValues {
    var kdownloaderTheme: KDownloaderTheme {
        get { self[KDownloaderThemeKey.self] }
        set { self[KDownloaderThemeKey.self] = newValue }
    }
}

public extension View {
    func kdownloaderTheme(_ theme: KDownloaderTheme) -> some View {
        environment(\.kdownloaderTheme, theme)
    }
}
