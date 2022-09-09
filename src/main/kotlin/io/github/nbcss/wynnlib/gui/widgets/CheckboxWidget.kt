package io.github.nbcss.wynnlib.gui.widgets

import io.github.nbcss.wynnlib.gui.TooltipScreen
import io.github.nbcss.wynnlib.i18n.Translatable.Companion.from
import io.github.nbcss.wynnlib.utils.Symbol
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class CheckboxWidget(private val posX: Int,
                     private val posY: Int,
                     private val name: Text,
                     private val screen: TooltipScreen? = null,
                     private var checked: Boolean = true):
    ClickableWidget(-1000, -1000, SIZE, SIZE, fromBoolean(checked)) {
    companion object {
        val LEFT_CLICK = from("wynnlib.ui.check_box.left_click")
        val RIGHT_CLICK = from("wynnlib.ui.check_box.right_click")
        const val SIZE = 20
        private fun fromBoolean(checked: Boolean): Text {
            return if (checked) Symbol.TICK.asText() else Symbol.CROSS.asText()
        }
    }
    private var group: Group? = null
    init {
        visible = false
    }

    fun setGroup(group: Group) {
        this.group = group
    }

    fun setChecked(checked: Boolean) {
        this.checked = checked
        message = fromBoolean(checked)
    }

    fun isChecked(): Boolean = checked

    fun updatePosition(x: Int, y: Int) {
        this.x = posX + x
        this.y = posY + y
        this.visible = true
    }

    override fun renderButton(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderButton(matrices, mouseX, mouseY, delta)
        if (hovered && screen != null) {
            val tooltip: MutableList<Text> = mutableListOf()
            tooltip.add((if (checked) Symbol.TICK.asText() else Symbol.CROSS.asText())
                .append(" ").append(name))
            tooltip.add(LiteralText.EMPTY)
            tooltip.add(LEFT_CLICK.formatted(Formatting.AQUA))
            if (group != null) {
                tooltip.add(RIGHT_CLICK.formatted(Formatting.LIGHT_PURPLE))
            }
            screen.drawTooltip(matrices!!, tooltip, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (this.visible) {
            if (clicked(mouseX, mouseY)) {
                if (button == 0) {
                    playDownSound(MinecraftClient.getInstance().soundManager)
                    setChecked(!isChecked())
                    return true
                }else if(button == 1 && group != null) {
                    playDownSound(MinecraftClient.getInstance().soundManager)
                    group?.onlySelect(this)
                    return true
                }
            }
        }
        return false
    }

    override fun appendNarrations(builder: NarrationMessageBuilder?) {
        appendDefaultNarrations(builder)
    }

    class Group(val widgets: Set<CheckboxWidget>) {
        fun onlySelect(widget: CheckboxWidget) {
            widgets.forEach { it.setChecked(it == widget) }
        }
    }
}