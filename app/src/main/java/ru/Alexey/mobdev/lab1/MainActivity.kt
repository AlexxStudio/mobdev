package ru.Alexey.mobdev.lab1

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView

    private var currentInput = "0"
    private var firstOperand: Double? = null
    private var pendingOperation: String? = null
    private var shouldClearOnNextDigit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

        if (savedInstanceState != null) {
            currentInput = savedInstanceState.getString("currentInput", "0")
            val savedOperand = savedInstanceState.getDouble("firstOperand", Double.NaN)
            firstOperand = if (savedOperand.isNaN()) null else savedOperand
            pendingOperation = savedInstanceState.getString("pendingOperation")
            shouldClearOnNextDigit =
                savedInstanceState.getBoolean("shouldClearOnNextDigit", false)
        }

        updateDisplay()
        initDigitButtons()
        initActionButtons()
    }

    private fun initDigitButtons() {
        val digitButtonIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        for (id in digitButtonIds) {
            findViewById<Button>(id).setOnClickListener {
                val digit = (it as Button).text.toString()
                appendDigit(digit)
            }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener {
            appendDot()
        }
    }

    private fun initActionButtons() {
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            clearAll()
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            setOperation("+")
        }

        findViewById<Button>(R.id.btnSub).setOnClickListener {
            setOperation("-")
        }

        findViewById<Button>(R.id.btnMul).setOnClickListener {
            setOperation("*")
        }

        findViewById<Button>(R.id.btnDiv).setOnClickListener {
            setOperation("/")
        }

        findViewById<Button>(R.id.btnEq).setOnClickListener {
            calculateResult()
        }
    }

    private fun appendDigit(digit: String) {
        currentInput = if (shouldClearOnNextDigit || currentInput == "0" || currentInput == "Error") {
            shouldClearOnNextDigit = false
            digit
        } else {
            currentInput + digit
        }
        updateDisplay()
    }

    private fun appendDot() {
        if (shouldClearOnNextDigit || currentInput == "Error") {
            currentInput = "0."
            shouldClearOnNextDigit = false
        } else if (!currentInput.contains(".")) {
            currentInput += "."
        }
        updateDisplay()
    }

    private fun setOperation(operation: String) {
        val value = currentInput.toDoubleOrNull() ?: return

        if (firstOperand != null && pendingOperation != null && !shouldClearOnNextDigit) {
            calculateResult()
        }

        firstOperand = value
        pendingOperation = operation
        shouldClearOnNextDigit = true
    }

    private fun calculateResult() {
        val first = firstOperand ?: return
        val second = currentInput.toDoubleOrNull() ?: return

        val result = when (pendingOperation) {
            "+" -> first + second
            "-" -> first - second
            "*" -> first * second
            "/" -> {
                if (second == 0.0) {
                    currentInput = "Error"
                    firstOperand = null
                    pendingOperation = null
                    shouldClearOnNextDigit = true
                    updateDisplay()
                    return
                }
                first / second
            }
            else -> return
        }

        currentInput = formatNumber(result)
        firstOperand = null
        pendingOperation = null
        shouldClearOnNextDigit = true
        updateDisplay()
    }

    private fun clearAll() {
        currentInput = "0"
        firstOperand = null
        pendingOperation = null
        shouldClearOnNextDigit = false
        updateDisplay()
    }

    private fun updateDisplay() {
        tvDisplay.text = currentInput
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        outState.putDouble("firstOperand", firstOperand ?: Double.NaN)
        outState.putString("pendingOperation", pendingOperation)
        outState.putBoolean("shouldClearOnNextDigit", shouldClearOnNextDigit)
    }
}