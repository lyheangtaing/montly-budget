import SwiftUI
import UIKit
import ComposeApp

@main
struct MonthlyBudgetApp: App {
    var body: some Scene {
        WindowGroup { BudgetView().ignoresSafeArea() }
    }
}

private struct BudgetView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
