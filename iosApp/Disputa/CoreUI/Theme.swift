import SwiftUI

enum DisputaTheme {
    static let blueberry = Color(red: 0.10, green: 0.13, blue: 0.28)
    static let surface = Color(red: 0.16, green: 0.20, blue: 0.40)
    static let mango = Color(red: 1.00, green: 0.67, blue: 0.22)
    static let lemon = Color(red: 0.90, green: 0.93, blue: 0.38)
    static let text = Color.white
    static let muted = Color.white.opacity(0.72)
}

struct ScreenBackground: ViewModifier {
    func body(content: Content) -> some View {
        ZStack {
            DisputaTheme.blueberry.ignoresSafeArea()
            content
        }
    }
}

extension View {
    func disputaScreen() -> some View {
        modifier(ScreenBackground())
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(DisputaTheme.blueberry)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(DisputaTheme.mango.opacity(configuration.isPressed ? 0.7 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct Card: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(16)
            .background(DisputaTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
