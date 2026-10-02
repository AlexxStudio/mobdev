import SwiftUI

struct ContentView: View {

    private let engine = CalculatorEngine()

    @State private var expression = ""
    @State private var result = "0"
    @State private var history: [String] = []
    @State private var memory: Double = 0
    @State private var errorMessage: String?

    private let buttons: [[String]] = [
        ["MC", "M+", "M−", "MR"],
        ["(", ")", "%", "⌫"],
        ["7", "8", "9", "÷"],
        ["4", "5", "6", "×"],
        ["1", "2", "3", "−"],
        ["±", "0", ".", "+"],
        ["C", "="]
    ]

    var body: some View {
        GeometryReader { geometry in
            if geometry.size.width > geometry.size.height {
                landscapeLayout
            } else {
                portraitLayout
            }
        }
    }


    // MARK: - Вертикальная ориентация

    private var portraitLayout: some View {
        VStack(spacing: 12) {

            historyView
                .frame(maxHeight: 100)

            Spacer()

            displayView

            Divider()

            buttonsView(buttonHeight: 55)
        }
        .padding()
    }


    // MARK: - Горизонтальная ориентация

    private var landscapeLayout: some View {
        HStack(spacing: 30) {

            // Левая часть
            VStack(spacing: 0) {

                historyView
                    .frame(maxHeight: 80)

                Spacer()

                VStack(spacing: 10) {
                    Text(expression.isEmpty ? "0" : expression)
                        .font(.system(size: 32, weight: .regular))
                        .lineLimit(2)
                        .minimumScaleFactor(0.5)

                    Text(errorMessage ?? result)
                        .font(.system(
                            size: errorMessage == nil ? 72 : 32,
                            weight: .bold
                        ))
                        .foregroundStyle(
                            errorMessage == nil
                            ? Color.primary
                            : Color.red
                        )
                        .lineLimit(errorMessage == nil ? 1 : 2)
                        .minimumScaleFactor(0.5)
                }
                .frame(maxWidth: .infinity)
                .offset(y: -25)

                Spacer()
            }
            .frame(maxWidth: .infinity)

            Divider()

            // Правая часть
            buttonsView(buttonHeight: 42)
                .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 30)
        .padding(.vertical, 12)
    }


    // MARK: - История

    private var historyView: some View {
        ScrollView {
            VStack(alignment: .trailing, spacing: 5) {
                ForEach(history, id: \.self) { item in
                    Button {
                        loadFromHistory(item)
                    } label: {
                        Text(item)
                            .font(.caption)
                            .foregroundStyle(Color.secondary)
                            .frame(
                                maxWidth: .infinity,
                                alignment: .trailing
                            )
                    }
                }
            }
        }
    }


    // MARK: - Экран калькулятора

    private var displayView: some View {
        VStack(spacing: 8) {

            Text(expression.isEmpty ? "0" : expression)
                .font(.title2)
                .lineLimit(2)
                .minimumScaleFactor(0.5)
                .frame(
                    maxWidth: .infinity,
                    alignment: .trailing
                )

            Text(errorMessage ?? result)
                .font(.system(size: 48, weight: .bold))
                .foregroundStyle(
                    errorMessage == nil
                    ? Color.primary
                    : Color.red
                )
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .frame(
                    maxWidth: .infinity,
                    alignment: .trailing
                )
        }
    }


    // MARK: - Кнопки

    private func buttonsView(buttonHeight: CGFloat) -> some View {
        VStack(spacing: 8) {
            ForEach(buttons, id: \.self) { row in
                HStack(spacing: 10) {
                    ForEach(row, id: \.self) { button in
                        Button {
                            handleButton(button)
                        } label: {
                            Text(button)
                                .font(.title2)
                                .frame(maxWidth: .infinity)
                                .frame(height: buttonHeight)
                                .background(Color.gray.opacity(0.2))
                                .foregroundStyle(Color.blue)
                                .clipShape(
                                    RoundedRectangle(cornerRadius: 12)
                                )
                        }
                    }
                }
            }
        }
    }


    // MARK: - Обработка кнопок

    private func handleButton(_ button: String) {
        errorMessage = nil

        switch button {

        case "C":
            expression = ""
            result = "0"

        case "⌫":
            if !expression.isEmpty {
                expression.removeLast()
            }

        case "=":
            calculate()

        case "M+":
            addToMemory()

        case "M−":
            subtractFromMemory()

        case "MR":
            recallMemory()

        case "MC":
            memory = 0

        case "±":
            toggleSign()

        case "%":
            applyPercent()

        default:
            appendSymbol(button)
        }
    }


    // MARK: - Память

    private func addToMemory() {
        do {
            let value = expression.isEmpty
                ? (Double(result) ?? 0)
                : try engine.calculate(expression)

            memory += value
        } catch {
            errorMessage = "Ошибка выражения"
        }
    }

    private func subtractFromMemory() {
        do {
            let value = expression.isEmpty
                ? (Double(result) ?? 0)
                : try engine.calculate(expression)

            memory -= value
        } catch {
            errorMessage = "Ошибка выражения"
        }
    }
    
    private func recallMemory() {
        let value = formatNumber(memory)

        if expression.isEmpty {
            expression = value
            result = value
        } else {
            expression += value
        }
    }


    // MARK: - Ввод

    private func appendSymbol(_ symbol: String) {
        let operators = ["+", "−", "×", "÷"]

        if operators.contains(symbol) {
            if expression.isEmpty {
                if symbol == "−" {
                    expression = "−"
                }
                return
            }

            if let last = expression.last,
               operators.contains(String(last)) {
                return
            }
        }

        if symbol == "." {
            let currentNumber = expression
                .components(
                    separatedBy: CharacterSet(
                        charactersIn: "+−×÷()"
                    )
                )
                .last ?? ""

            if currentNumber.contains(".") {
                return
            }

            if currentNumber.isEmpty {
                expression += "0"
            }
        }

        expression += symbol
    }

    private func appendNumber(_ number: String) {
        expression += number
    }


    // MARK: - Вычисление

    private func calculate() {
        guard !expression.isEmpty else {
            errorMessage = "Пустое выражение"
            return
        }

        do {
            let value = try engine.calculate(expression)
            let formatted = formatNumber(value)

            result = formatted

            let historyItem = "\(expression) = \(formatted)"

            history.insert(historyItem, at: 0)

            if history.count > 10 {
                history.removeLast()
            }

        } catch CalculatorError.divisionByZero {
            errorMessage = "Деление на ноль"
        } catch CalculatorError.emptyExpression {
            errorMessage = "Пустое выражение"
        } catch CalculatorError.parenthesesError {
            errorMessage = "Ошибка в скобках"
        } catch CalculatorError.unfinishedExpression {
            errorMessage = "Выражение не завершено"
        } catch {
            errorMessage = "Ошибка выражения"
        }
    }


    // MARK: - Процент

    private func applyPercent() {
        guard !expression.isEmpty else {
            return
        }

        do {
            let value = try engine.calculate(expression)
            let percent = value / 100
            expression = formatNumber(percent)
            result = expression
        } catch {
            errorMessage = "Ошибка выражения"
        }
    }


    // MARK: - Смена знака

    private func toggleSign() {
        guard !expression.isEmpty else {
            return
        }

        if expression.hasPrefix("−") {
            expression.removeFirst()
        } else {
            expression = "−" + expression
        }

        do {
            let value = try engine.calculate(expression)
            result = formatNumber(value)
        } catch {
            result = "0"
        }
    }


    // MARK: - История

    private func loadFromHistory(_ item: String) {
        if let range = item.range(of: " = ") {
            expression = String(item[..<range.lowerBound])
            result = String(item[range.upperBound...])
        }
    }


    // MARK: - Формат числа

    private func formatNumber(_ value: Double) -> String {
        if value.rounded() == value {
            return String(Int(value))
        }

        return String(value)
    }
}

#Preview {
    ContentView()
}
