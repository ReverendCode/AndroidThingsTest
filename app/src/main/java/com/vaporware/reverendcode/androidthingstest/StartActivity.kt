package com.vaporware.reverendcode.androidthingstest

import android.accounts.OnAccountsUpdateListener
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.pwmspeaker.Speaker
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import com.google.android.things.pio.GpioCallback
import java.io.Closeable
import kotlin.properties.Delegates
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import kotlin.reflect.KAnnotatedElement


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class StartActivity : Activity() {
    val TAG = "HOME"
    val LED_A_GPIO_PIN = "GPIO_34"
    val BUTTON_A_GPIO_PIN = "GPIO_174"
    val BUTTON_B_GPIO_PIN = "GPIO_175"
    val BUTTON_C_GPIO_PIN = "GPIO_39"


    var inputController by Delegates.notNull<ButtonController>()
    var outputController by Delegates.notNull<OutputController>()

    var mLedGpio by Delegates.notNull<Gpio>()

    private val mCallback = object : GpioCallback() {
        override fun onGpioEdge(gpio: Gpio?): Boolean {
                Log.d(TAG, "GPIO changed, button " + gpio!!.value)
                mLedGpio.value = gpio.value
            // Return true to keep callback active.
            return true
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        val service = PeripheralManagerService()
        Log.d("home","Ports found: " + service.gpioList)
        inputController = ButtonController()
        outputController = OutputController()

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (outputController.processInput(keyCode, true)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (outputController.processInput(keyCode, false)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }


    override fun onDestroy() {
        super.onDestroy()

        if (mLedGpio != null) {
            mLedGpio?.close()
        }
    }
}

class ButtonController(private val buttonDrivers: List<ButtonInputDriver> = listOf(
        ButtonController.registerButton(ButtonController.BUTTON_A_GPIO_PIN, KeyEvent.KEYCODE_A),
        ButtonController.registerButton(ButtonController.BUTTON_B_GPIO_PIN, KeyEvent.KEYCODE_B),
        ButtonController.registerButton(ButtonController.BUTTON_C_GPIO_PIN, KeyEvent.KEYCODE_C))): Closeable {

    companion object {
        val BUTTON_A_GPIO_PIN = "GPIO_174"
        val BUTTON_B_GPIO_PIN = "GPIO_175"
        val BUTTON_C_GPIO_PIN = "GPIO_39"

        private fun registerButton(pin: String, keyCode: Int): ButtonInputDriver {
            val temp = ButtonInputDriver(pin, Button.LogicState.PRESSED_WHEN_LOW, keyCode)
            temp.register()
            return temp
        }
    }

    override fun close() {
//        todo: these things might want to be in a try//catch block
        buttonDrivers.forEach(ButtonInputDriver::unregister)
        buttonDrivers.forEach(ButtonInputDriver::close)

    }

}

class OutputController(val outputMap: Map<Int,Gpio> = mapOf<Int, Gpio>(
        KeyEvent.KEYCODE_A to RainbowHat.openLedRed(),
        KeyEvent.KEYCODE_B to RainbowHat.openLedGreen(),
        KeyEvent.KEYCODE_C to RainbowHat.openLedBlue()),
                       val buzzer: Speaker = RainbowHat.openPiezo()): Closeable {

    companion object {
        val soundMap = mapOf<Int, Double>(
                KeyEvent.KEYCODE_A to 100.0,
                KeyEvent.KEYCODE_B to 440.0,
                KeyEvent.KEYCODE_C to 770.0
        )
    }

    fun processInput(keyCode: Int, keyDown: Boolean): Boolean {
        val temp = outputMap[keyCode] ?: return false
        temp.value = keyDown
        if (keyDown) {
            this.buzzer.play(soundMap[keyCode]?: 1000.0)
        } else this.buzzer.stop()


        return true
    }

    override fun close() {
        outputMap.forEach { _, gpio -> gpio.close() }
        buzzer.close()
    }
}

