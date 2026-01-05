import SwiftUI

#if canImport(Lottie)
import Lottie
#endif

struct LottieView: View {
    enum LoopMode {
        case loop
        case playOnce
    }

    enum Scaling {
        case fit
        case fill
        case stretch
    }

    let name: String
    var loopMode: LoopMode = .loop
    var animationSpeed: CGFloat = 1
    var scaling: Scaling = .fit

    var body: some View {
        Group {
#if canImport(Lottie)
            LottieRepresentable(
                name: name,
                loopMode: loopMode.lottieLoopMode,
                animationSpeed: animationSpeed,
                scaling: scaling.contentMode
            )
#else
            ProgressView()
#endif
        }
    }
}

#if canImport(Lottie)
private struct LottieRepresentable: UIViewRepresentable {
    let name: String
    let loopMode: LottieLoopMode
    let animationSpeed: CGFloat
    let scaling: UIView.ContentMode

    func makeUIView(context: Context) -> UIView {
        let container = UIView()
        container.backgroundColor = .clear
        container.clipsToBounds = false

        let animationView = LottieAnimationView(name: name)
        animationView.backgroundColor = .clear
        animationView.clipsToBounds = false
        animationView.translatesAutoresizingMaskIntoConstraints = false
        animationView.loopMode = loopMode
        animationView.animationSpeed = animationSpeed
        animationView.backgroundBehavior = .pauseAndRestore
        animationView.contentMode = scaling
        animationView.tag = 999

        container.addSubview(animationView)
        NSLayoutConstraint.activate([
            animationView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            animationView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            animationView.topAnchor.constraint(equalTo: container.topAnchor),
            animationView.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])

        animationView.play()
        return container
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        guard let animationView = uiView.viewWithTag(999) as? LottieAnimationView else {
            return
        }

        animationView.loopMode = loopMode
        animationView.animationSpeed = animationSpeed
        animationView.contentMode = scaling

        if !animationView.isAnimationPlaying {
            animationView.play()
        }
    }
}

private extension LottieView.LoopMode {
    var lottieLoopMode: LottieLoopMode {
        switch self {
        case .loop:
            return .loop
        case .playOnce:
            return .playOnce
        }
    }
}

private extension LottieView.Scaling {
    var contentMode: UIView.ContentMode {
        switch self {
        case .fit:
            return .scaleAspectFit
        case .fill:
            return .scaleAspectFill
        case .stretch:
            return .scaleToFill
        }
    }
}
#endif
