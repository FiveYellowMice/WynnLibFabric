package io.github.nbcss.wynnlib.gui

import com.mojang.blaze3d.systems.RenderSystem
import io.github.nbcss.wynnlib.abilities.Ability
import io.github.nbcss.wynnlib.abilities.AbilityTree
import io.github.nbcss.wynnlib.data.CharacterClass
import io.github.nbcss.wynnlib.i18n.Translations
import io.github.nbcss.wynnlib.registry.AbilityRegistry
import io.github.nbcss.wynnlib.render.RenderKit
import io.github.nbcss.wynnlib.utils.Color
import io.github.nbcss.wynnlib.utils.ItemFactory
import io.github.nbcss.wynnlib.utils.Pos
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class AbilityTreeViewerScreen(parent: Screen?) : HandbookTabScreen(parent, TITLE) {
    private val texture = Identifier("wynnlib", "textures/gui/ability_tree_viewer.png")
    companion object {
        val ICON: ItemStack = ItemFactory.fromEncoding("minecraft:stone_axe#83")
        val TITLE: Text = Translations.UI_ABILITY_TREE.translate()
        val FACTORY = object: TabFactory {
            override fun getTabIcon(): ItemStack = ICON
            override fun getTabTitle(): Text = TITLE
            override fun createScreen(parent: Screen?): HandbookTabScreen = AbilityTreeViewerScreen(parent)
            override fun isInstance(screen: HandbookTabScreen): Boolean = screen is AbilityTreeViewerScreen
        }
        const val GRID_SIZE: Int = 24
        const val VIEW_WIDTH = 232
        const val VIEW_HEIGHT = 138
        const val SCROLL_FRAMES = 4
        const val ACTIVE_OUTER_COLOR: Int = 0x37ACB5 + (0xFF shl 24)
        const val ACTIVE_INNER_COLOR: Int = 0x5FD6DF + (0xFF shl 24)
        const val LOCKED_OUTER_COLOR: Int = 0x2D2E30 + (0xFF shl 24)
        const val LOCKED_INNER_COLOR: Int = 0x252527 + (0xFF shl 24)
        const val BASIC_OUTER_COLOR: Int = 0xEEEEEE + (0xFF shl 24)
        const val BASIC_INNER_COLOR: Int = 0xAAAAAA + (0xFF shl 24)
    }
    private var tree: AbilityTree = AbilityRegistry.fromCharacter(CharacterClass.WARRIOR)
    private var viewerX: Int = 0
    private var viewerY: Int = 0
    private var scroll: Int = 0
    private var movingScroll: Int = 0
    private var scrollTicks: Int = 0

    private fun drawOuterEdge(matrices: MatrixStack, from: Pos, to: Pos, color: Int, reroute: Boolean){
        RenderSystem.enableDepthTest()
        if (from.x != to.x){
            val posY = if (reroute) to.y else from.y
            DrawableHelper.fill(matrices, from.x, posY - 2, to.x, posY + 2, color)
        }
        if (from.y != to.y){
            val fromY = from.y + if (from.x == to.x || reroute) 0 else -2
            val toY = to.y + if (from.x != to.x && reroute) 2 else 0
            val posX = if (reroute) from.x else to.x
            DrawableHelper.fill(matrices, posX - 2, fromY, posX + 2, toY, color)
        }
    }
    private fun drawInnerEdge(matrices: MatrixStack, from: Pos, to: Pos, color: Int, reroute: Boolean){
        if (from.x != to.x){
            val posY = if (reroute) to.y else from.y
            DrawableHelper.fill(matrices, from.x, posY - 1, to.x, posY + 1, color)
        }
        if (from.y != to.y){
            val fromY = from.y + if (from.x == to.x || reroute) 0 else -1
            val toY = to.y + if (from.x != to.x && reroute) 1 else 0
            val posX = if (reroute) from.x else to.x
            DrawableHelper.fill(matrices, posX - 1, fromY, posX + 1, toY, color)
        }
    }

    private fun toScreenPosition(height: Int, position: Int): Pos {
        val posX = windowX + 9 + GRID_SIZE / 2 + (position + 4) * GRID_SIZE
        val posY = windowY + 47 + GRID_SIZE / 2 + height * GRID_SIZE - movingScroll
        return Pos(posX, posY)
    }

    private fun drawCharacterTab(matrices: MatrixStack, index: Int, mouseX: Int, mouseY: Int) {
        val posX = windowX + 242
        val posY = windowY + 44 + index * 28
        val v = if (tree.character.ordinal == index) 172 else 144
        RenderKit.renderTexture(matrices, texture, posX, posY, 0, v, 32, 28)
        val character = CharacterClass.values()[index]
        val icon = character.getWeapon().getIcon()
        itemRenderer.renderInGuiWithOverrides(icon, posX + 7, posY + 6)
        if (isOverCharacterTab(index, mouseX, mouseY)){
            drawTooltip(matrices, listOf(character.translate()), mouseX, mouseY)
        }
    }

    private fun isOverCharacterTab(index: Int, mouseX: Int, mouseY: Int): Boolean {
        val posX = windowX + 245
        val posY = windowY + 44 + index * 28
        return mouseX >= posX && mouseX < posX + 29 && mouseY >= posY && mouseY < posY + 28
    }

    private fun isOverViewer(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= viewerX && mouseX < viewerX + VIEW_WIDTH && mouseY >= viewerY && mouseY < viewerY + VIEW_HEIGHT
    }

    private fun renderEdges(abilities: List<Ability>, matrices: MatrixStack, color: Int, inner: Boolean){
        abilities.forEach {
            val to = toScreenPosition(it.getHeight(), it.getPosition())
            it.getPredecessors().forEach { node ->
                val from = toScreenPosition(node.getHeight(), node.getPosition())
                val height = min(it.getHeight(), node.getHeight())
                val reroute = tree.getAbilityFromPosition(height, it.getPosition()) != null
                if (inner) {
                    drawInnerEdge(matrices, from, to, color, reroute)
                }else{
                    drawOuterEdge(matrices, from, to, color, reroute)
                }
            }
        }
    }

    override fun init() {
        super.init()
        viewerX = windowX + 7
        viewerY = windowY + 45
    }

    override fun getTitle(): Text {
        return title.copy().append(" [").append(tree.character.translate()).append("]")
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (isOverViewer(mouseX.toInt(), mouseY.toInt())){
            val max = max(0, 4 + (1 + tree.getMaxHeight()) * GRID_SIZE - VIEW_HEIGHT)
            scroll = MathHelper.clamp(scroll - amount.toInt() * GRID_SIZE, 0, max)
            scrollTicks = SCROLL_FRAMES
        }
        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun drawBackgroundPre(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        super.drawBackgroundPre(matrices, mouseX, mouseY, delta)
        (0 until CharacterClass.values().size)
            .filter { CharacterClass.values()[it] != tree.character }
            .forEach { drawCharacterTab(matrices!!, it, mouseX, mouseY) }
    }

    override fun drawBackgroundPost(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        super.drawBackgroundPost(matrices, mouseX, mouseY, delta)
        drawCharacterTab(matrices!!, tree.character.ordinal, mouseX, mouseY)
        RenderKit.renderTexture(matrices, texture, windowX + 4, windowY + 42, 0, 0, 238, 144)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        CharacterClass.values()
            .firstOrNull {isOverCharacterTab(it.ordinal, mouseX.toInt(), mouseY.toInt())}?.let {
                this.tree = AbilityRegistry.fromCharacter(it)
                this.scroll = 0
                this.movingScroll = 0
                this.scrollTicks = 0
                return true
            }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun drawContents(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        var archetypeX = viewerX + 2
        val archetypeY = viewerY + 143
        //render archetype values
        tree.getArchetypes().forEach {
            val icon = it.getTexture()
            val iconText = it.getIconText()
            val points = tree.getArchetypePoint(it).toString()
            itemRenderer.renderInGuiWithOverrides(icon, archetypeX, archetypeY)
            itemRenderer.renderGuiItemOverlay(textRenderer, icon, archetypeX, archetypeY, iconText)
            textRenderer.draw(matrices, points, archetypeX.toFloat() + 20, archetypeY.toFloat() + 4, 0)
            if (mouseX >= archetypeX && mouseY >= archetypeY && mouseX <= archetypeX + 16 && mouseY <= archetypeY + 16){
                drawTooltip(matrices!!, it.getTooltip(), mouseX, mouseY)
            }
            archetypeX += 60
        }
        //update moving scroll
        if (scrollTicks > 0){
            movingScroll += ((scroll - movingScroll).toFloat() / scrollTicks.toFloat()).toInt()
            scrollTicks -= 1
        }else {
            movingScroll = scroll
        }
        val bottom = (viewerY + VIEW_HEIGHT)
        val scale = client!!.window.scaleFactor
        tree.getAbilities().forEach { ability ->
            ability.getArchetype()?.let {
                val node = toScreenPosition(ability.getHeight(), ability.getPosition())
                val item = ability.getTier().getLockedTexture()
                val color = Color.fromFormatting(it.getFormatting())
                val itemX = node.x - 15
                val itemY = node.y - 15
                matrices!!.push()
                //todo not sure how to make outline item color in 1.18
                //RenderSystem.enableBlend()
                //WorldRenderEvents
                //RenderSystem.texParameter()
                //RenderSystem.texParameter()

                //RenderSystem.setShader { GameRenderer.getPositionTexColorShader() }
                //val buffer = Tessellator.getInstance().buffer
                //buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
                val r = color.red / 255.0f
                val g = color.green / 255.0f
                val b = color.blue / 255.0f
                val tex = Identifier("wynnlib", "textures/gui/tabs.png")
                //RenderKit.renderTexture(matrices, tex, itemX, itemY, 0, 0, 30, 30, 256, 256)
                //RenderSystem.enableTexture()
                //RenderSystem.enableBlend()

                //RenderSystem.setShaderColor(r, g, b, 1.0f)
                //itemRenderer.renderInGui(item, itemX, itemY - 30)
                /*RenderSystem.setShader { GameRenderer.getPositionColorTexShader() }
                val tex = Identifier("wynnlib", "dictionary_slots_base.png")
                RenderSystem.setShaderTexture(0, tex)
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                val buffer = Tessellator.getInstance().buffer
                buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE)
                buffer.vertex(matrices.peek().positionMatrix, itemX.toFloat(), itemY.toFloat(), 300.0f)
                    .color(0x66, 0xCC, 0xFF, 255).texture(0.0f, 0.0f).next()
                buffer.vertex(matrices.peek().positionMatrix, itemX.toFloat(), itemY.toFloat() + 30, 300.0f)
                    .color(0x66, 0xCC, 0xFF, 255).texture(0.0f, 0.0f).next()
                buffer.vertex(matrices.peek().positionMatrix, itemX.toFloat() + 30, itemY.toFloat() + 30, 300.0f)
                    .color(0x66, 0xCC, 0xFF, 255).texture(0.0f, 0.0f).next()
                buffer.vertex(matrices.peek().positionMatrix, itemX.toFloat() + 30, itemY.toFloat(), 300.0f)
                    .color(0x66, 0xCC, 0xFF, 255).texture(0.0f, 0.0f).next()
                Tessellator.getInstance().draw()*/
                //OutlineItemVertexProvider.setColor(color.red, color.green, color.blue, 255)
                //OutlineItemVertexProvider.renderItem(item, itemX, itemY - 30)
                //itemRenderer.renderGuiItemIcon(item, itemX, itemY - 30)
                /*RenderSystem.setShaderTexture(0, texture)
                buffer.vertex(itemX.toDouble(), itemY.toDouble() + 20, 200.0)
                    .texture(0.0f, 10.0f)
                    .color(r, g, b, 1.0f).next()
                buffer.vertex(itemX.toDouble() + 20, itemY.toDouble() + 20, 200.0)
                    .texture(10.0f, 10.0f)
                    .color(r, g, b, 1.0f).next()
                buffer.vertex(itemX.toDouble() + 20, itemY.toDouble(), 200.0)
                    .texture(10.0f, 0.0f)
                    .color(r, g, b, 1.0f).next()
                buffer.vertex(itemX.toDouble(), itemY.toDouble(), 200.0)
                    .texture(0.0f, 0.0f)
                    .color(r, g, b, 1.0f).next()*/
                //itemRenderer.renderInGui()
                //buffer.end()
                matrices.pop()
            }
        }
        RenderSystem.enableScissor((viewerX * scale).toInt(),
            client!!.window.height - (bottom * scale).toInt(),
            (VIEW_WIDTH * scale).toInt(), (VIEW_HEIGHT * scale).toInt())
        //render node background
        //render inactive edges (basic)
        val inactive = tree.getAbilities().filter {
            !isOverViewer(mouseX, mouseY) || !isOverNode(
                toScreenPosition(it.getHeight(), it.getPosition()), mouseX, mouseY)
        }
        renderEdges(inactive, matrices!!, LOCKED_OUTER_COLOR, false)
        renderEdges(inactive, matrices, LOCKED_INNER_COLOR, true)
        //render active edges
        val active = tree.getAbilities().filter {
            isOverViewer(mouseX, mouseY) && isOverNode(toScreenPosition(it.getHeight(), it.getPosition()), mouseX, mouseY)
        }
        renderEdges(active, matrices, ACTIVE_OUTER_COLOR, false)
        renderEdges(active, matrices, ACTIVE_INNER_COLOR, true)
        //render icons
        tree.getAbilities().forEach {
            val node = toScreenPosition(it.getHeight(), it.getPosition())
            if (it.getTier().getLevel() != 0){
                it.getArchetype()?.let { arch ->
                    val color = Color.fromFormatting(arch.getFormatting())
                    val itemX = node.x - 15
                    val itemY = node.y - 15
                    val u = 32 + 30 * (it.getTier().getLevel() - 1)
                    RenderSystem.enableBlend()
                    RenderKit.renderTextureWithColor(matrices, texture, color, 165, itemX, itemY,
                        u, 144, 30, 30, 256, 256)
                }
            }
            val item = if (isOverViewer(mouseX, mouseY) && isOverNode(node, mouseX, mouseY))
                it.getTier().getActiveTexture() else it.getTier().getUnlockedTexture()
            itemRenderer.renderInGuiWithOverrides(item, node.x - 8, node.y - 8)
        }
        RenderSystem.disableScissor()
        //render ability tooltip
        if (isOverViewer(mouseX, mouseY)){
            for (ability in tree.getAbilities()) {
                val node = toScreenPosition(ability.getHeight(), ability.getPosition())
                if (isOverNode(node, mouseX, mouseY)){
                    drawTooltip(matrices, ability.getTooltip(), mouseX, mouseY + 20)
                    break
                }
            }
        }
    }

    private fun isOverNode(node: Pos, mouseX: Int, mouseY: Int): Boolean {
        return abs(node.x - mouseX) <= 11 && abs(node.y - mouseY) <= 11
    }
}