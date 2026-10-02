import Foundation

enum CalculatorError: Error {
    case emptyExpression
    case invalidExpression
    case divisionByZero
    case parenthesesError
    case unfinishedExpression
}

struct CalculatorEngine {

    func calculate(_ expression: String) throws -> Double {
        let expression = expression
            .replacingOccurrences(of: "×", with: "*")
            .replacingOccurrences(of: "÷", with: "/")
            .replacingOccurrences(of: "−", with: "-")
            .replacingOccurrences(of: ",", with: ".")
            .replacingOccurrences(of: " ", with: "")

        guard !expression.isEmpty else {
            throw CalculatorError.emptyExpression
        }
        
        if let last = expression.last,
           "+-*/".contains(last) {
            throw CalculatorError.unfinishedExpression
        }

        return try evaluate(expression)
    }

    private func evaluate(_ expression: String) throws -> Double {
        var index = expression.startIndex

        func parseExpression() throws -> Double {
            var value = try parseTerm()

            while index < expression.endIndex {
                let char = expression[index]

                if char == "+" {
                    index = expression.index(after: index)
                    value += try parseTerm()
                } else if char == "-" {
                    index = expression.index(after: index)
                    value -= try parseTerm()
                } else {
                    break
                }
            }

            return value
        }

        func parseTerm() throws -> Double {
            var value = try parseFactor()

            while index < expression.endIndex {
                let char = expression[index]

                if char == "*" {
                    index = expression.index(after: index)
                    value *= try parseFactor()
                } else if char == "/" {
                    index = expression.index(after: index)

                    let divisor = try parseFactor()

                    if divisor == 0 {
                        throw CalculatorError.divisionByZero
                    }

                    value /= divisor
                } else {
                    break
                }
            }

            return value
        }

        func parseFactor() throws -> Double {
            guard index < expression.endIndex else {
                throw CalculatorError.invalidExpression
            }

            // Унарный минус
            if expression[index] == "-" {
                index = expression.index(after: index)
                return -(try parseFactor())
            }

            // Унарный плюс
            if expression[index] == "+" {
                index = expression.index(after: index)
                return try parseFactor()
            }

            // Скобки
            if expression[index] == "(" {
                index = expression.index(after: index)

                let value = try parseExpression()

                guard index < expression.endIndex,
                      expression[index] == ")" else {
                    throw CalculatorError.parenthesesError
                }

                index = expression.index(after: index)
                return value
            }

            // Число
            let start = index
            var dotCount = 0

            while index < expression.endIndex {
                let char = expression[index]

                if char.isNumber {
                    index = expression.index(after: index)
                } else if char == "." {
                    dotCount += 1

                    if dotCount > 1 {
                        throw CalculatorError.invalidExpression
                    }

                    index = expression.index(after: index)
                } else {
                    break
                }
            }

            guard start != index else {
                throw CalculatorError.invalidExpression
            }

            let numberString = String(expression[start..<index])

            guard let number = Double(numberString) else {
                throw CalculatorError.invalidExpression
            }

            return number
        }

        let result = try parseExpression()

        guard index == expression.endIndex else {
            throw CalculatorError.invalidExpression
        }

        return result
    }
}
