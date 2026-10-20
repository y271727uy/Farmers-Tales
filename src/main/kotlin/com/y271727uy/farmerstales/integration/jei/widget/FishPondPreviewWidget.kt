package com.y271727uy.farmerstales.integration.jei.widget

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup
import com.lowdragmc.lowdraglib.gui.texture.TextTexture
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.jei.IngredientIO
import com.lowdragmc.lowdraglib.utils.BlockInfo
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld

import com.y271727uy.farmerstales.integration.jei.FishPondStructureRecipe
import com.y271727uy.farmerstales.integration.list.FishPondPattern
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
 * 珠海渔场的 ldlib 结构预览。拖拽旋转、滚轮缩放、分层，底部是材料数量。
 */
@OnlyIn(Dist.CLIENT)
class FishPondPreviewWidget private constructor(private val index: Int) : WidgetGroup(0, 0, WIDTH, HEIGHT) {
    private val sceneWidget: SceneWidget
    private val materials: DraggableScrollableWidgetGroup
    private var layer = -1
    private var slots = emptyArray<SlotWidget>()

    init {
        setClientSideWidget()
        ensureWorld()

        sceneWidget = SceneWidget(3, 12, 150, SCENE_HEIGHT, world)
            .setRenderFacing(false)
            .setHoverTips(true)
        addWidget(sceneWidget)

        addWidget(
            ImageWidget(
                3, 1, 148, 11,
                TextTexture("", -1)
                    .setSupplier { title() }
                    .setType(TextTexture.TextType.ROLL)
                    .setWidth(148)
                    .setDropShadow(true),
            ),
        )
        addWidget(
            ButtonWidget(
                156, 32, 18, 18,
                GuiTextureGroup(ColorPattern.T_GRAY.rectTexture(), TextTexture("").setSupplier { layerLabel() }),
                { switchLayer() },
            ).setHoverBorderTexture(1, -1),
        )

        materials = DraggableScrollableWidgetGroup(3, 14 + SCENE_HEIGHT, 171, 20)
            .setXScrollBarHeight(3)
            .setXBarStyle(ColorPattern.T_GRAY.rectTexture(), ColorPattern.WHITE.rectTexture())
            .setDraggable(true)
            .setScrollable(true)
        materials.setScrollWheelDirection(DraggableScrollableWidgetGroup.ScrollWheelDirection.HORIZONTAL)
        slots = createSlots(patterns[index].parts)
        slots.forEach(materials::addWidget)
        addWidget(materials)
        show(-1)
    }

    private fun switchLayer() {
        val pattern = patterns[index]
        val next = layer + 1
        show(if (next > pattern.maxY - pattern.minY) -1 else next)
    }

    override fun updateScreen() {
        super.updateScreen()
        // 切配方页时 JEI 不会把松开鼠标转给旧页，场景会一直停在拖拽里。
        if (sceneWidget.isDragging && !isMouseDown()) {
            sceneWidget.mouseReleased(0.0, 0.0, 0)
        }
    }

    private fun isMouseDown(): Boolean {
        val window = Minecraft.getInstance().window.window
        return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
            || org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
    }

    private fun show(nextLayer: Int) {
        layer = nextLayer
        val pattern = patterns[index]
        val visible = pattern.blocks.keys.filter { pos ->
            layer < 0 || pos.y == pattern.origin.y + pattern.minY + layer
        }
        sceneWidget.setRenderedCore(visible)
        sceneWidget.center?.let { center ->
            sceneWidget.setCenter(org.joml.Vector3f(center.x, center.y + 0.5f, center.z))
        }
    }

    private fun title(): String {
        return Component.translatable("block.list.fishpond_core").string + " " + pageLabel()
    }

    private fun pageLabel(): String {
        val key = if (patterns[index].id == FishPondPattern.LAVA) {
            "gui.farmerstales.fish_pond.page.lava"
        } else {
            "gui.farmerstales.fish_pond.page.water"
        }
        return Component.translatable(key).string
    }

    private fun layerLabel(): String {
        return if (layer < 0) Component.translatable("gui.farmerstales.fish_pond.layer_all").string else "Y$layer"
    }

    private class Pattern(
        val id: String,
        val origin: BlockPos,
        val blocks: Map<BlockPos, BlockState>,
        val minY: Int,
        val maxY: Int,
        val parts: List<List<ItemStack>>,
    )

    companion object {
        const val WIDTH = 177
        const val SCENE_HEIGHT = 112
        const val HEIGHT = 14 + SCENE_HEIGHT + 22

        private var world: TrackedDummyWorld? = null
        private lateinit var patterns: List<Pattern>

        fun create(recipe: FishPondStructureRecipe): FishPondPreviewWidget {
            check(Minecraft.getInstance().level != null) { "Fish pond preview opened before the client level loaded" }
            ensureWorld()
            val page = patterns.indexOfFirst { it.id == recipe.pattern }.coerceAtLeast(0)
            return FishPondPreviewWidget(page)
        }

        private fun ensureWorld() {
            if (world != null) {
                return
            }
            val previewWorld = fluidAwareWorld()
            world = previewWorld
            var cursor = BlockPos(0, 80, 0)
            patterns = FishPondPattern.PAGES.mapNotNull { id ->
                val blocks = FishPondPattern.preview(id) ?: return@mapNotNull null
                val placed = LinkedHashMap<BlockPos, BlockState>()
                var minY = Int.MAX_VALUE
                var maxY = Int.MIN_VALUE
                for (block in blocks) {
                    val pos = cursor.offset(block.offset)
                    placed[pos] = block.state
                    previewWorld.addBlock(pos, BlockInfo(block.state))
                    minY = minOf(minY, block.offset.y)
                    maxY = maxOf(maxY, block.offset.y)
                }
                val parts = FishPondPattern.materials(id).map { listOf(it) }
                val pattern = Pattern(id, cursor, placed, minY, maxY, parts)
                cursor = cursor.offset(0, 0, 32)
                pattern
            }
        }

        /**
         * ldlib 的 DummyWorld 把流体状态固定成空，水池和熔岩池不会画出来。
         * 用 Java 子类绕开 Kotlin 把这个类看成抽象类的问题。
         */
        private fun fluidAwareWorld(): TrackedDummyWorld {
            return FishPondPreviewWorld()
        }

        private fun createSlots(parts: List<List<ItemStack>>): Array<SlotWidget> {
            val handler = CycleItemStackHandler(parts)
            return Array(parts.size) { index ->
                SlotWidget(handler, index, 4 + index * 18, 0, false, false)
                    .setIngredientIO(IngredientIO.INPUT)
            }
        }
    }
}
