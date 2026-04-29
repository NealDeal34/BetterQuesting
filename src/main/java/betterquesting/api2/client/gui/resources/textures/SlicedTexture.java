package betterquesting.api2.client.gui.resources.textures;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonObject;

import betterquesting.api.utils.JsonHelper;
import betterquesting.api2.client.gui.misc.GuiPadding;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.resources.colors.IGuiColor;
import cpw.mods.fml.client.config.GuiUtils;

public class SlicedTexture implements IGuiTexture {

    private static final IGuiColor defColor = new GuiColorStatic(255, 255, 255, 255);
    private final ResourceLocation texture;
    private final IGuiRect texBounds;
    private final GuiPadding texBorder;
    private SliceMode sliceMode = SliceMode.SLICED_TILE;

    public SlicedTexture(ResourceLocation tex, IGuiRect bounds, GuiPadding border) {
        this.texture = tex;
        this.texBounds = bounds;
        this.texBorder = border;
    }

    // ========== 🔍 调试工具方法 ==========
    /**
     * 检查并打印 OpenGL 错误（带标签 + 调用栈）
     * 注意：glGetError() 会清除当前错误标志，调试完毕后建议移除或注释掉 Thread.dumpStack()
     */
    private void checkGlError(String tag) {
        int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            String errName;
            switch (err) {
                case GL11.GL_INVALID_ENUM:      errName = "GL_INVALID_ENUM(1280)"; break;
                case GL11.GL_INVALID_VALUE:     errName = "GL_INVALID_VALUE(1281)"; break;
                case GL11.GL_INVALID_OPERATION: errName = "GL_INVALID_OPERATION(1282)"; break;
                case GL11.GL_STACK_OVERFLOW:    errName = "GL_STACK_OVERFLOW(1283)"; break;
                case GL11.GL_STACK_UNDERFLOW:   errName = "GL_STACK_UNDERFLOW(1284)"; break;
                case GL11.GL_OUT_OF_MEMORY:     errName = "GL_OUT_OF_MEMORY(1285)"; break;
                default: errName = "Unknown(" + err + ")";
            }
            System.err.println("❌ [BQ SlicedTexture] GL Error after " + tag + ": " + errName);
            // 🔍 打印调用栈，精准定位是哪个模组/类留下了错误
            Thread.dumpStack();
        }
    }
    // ================================

    @Override
    public void drawTexture(int x, int y, int width, int height, float zLevel, float partialTick) {
        drawTexture(x, y, width, height, zLevel, partialTick, defColor);
    }

    @Override
    public void drawTexture(int x, int y, int width, int height, float zLevel, float partialTick, IGuiColor color) {
        if (width <= 0 || height <= 0) return;
        if (texture == null) return;

        // 🛡️ 防御性：清空可能由上游渲染遗留的单个错误，避免误判为本方法问题
        GL11.glGetError();

        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT);
        checkGlError("glPushAttrib");
        
        GL11.glPushMatrix();
        checkGlError("glPushMatrix");

        try {
            int w = Math.max(width, texBorder.getLeft() + texBorder.getRight());
            int h = Math.max(height, texBorder.getTop() + texBorder.getBottom());
            int dx = x;
            int dy = y;

            applyGlColorSafe(color);
            checkGlError("applyGlColorSafe");

            GL11.glEnable(GL11.GL_BLEND);
            checkGlError("glEnable(BLEND)");
            
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            checkGlError("glBlendFunc");

            if (w != width || h != height) {
                dx = 0;
                dy = 0;
                GL11.glTranslatef(x, y, 0);
                checkGlError("glTranslatef(scale-offset)");
                
                GL11.glScaled(width / (double) w, height / (double) h, 1D);
                checkGlError("glScaled");
            }

            if (sliceMode == SliceMode.SLICED_TILE) {
                drawContinuousTexturedBox(
                    texture,
                    dx,
                    dy,
                    texBounds.getX(),
                    texBounds.getY(),
                    w,
                    h,
                    texBounds.getWidth(),
                    texBounds.getHeight(),
                    texBorder.getTop(),
                    texBorder.getBottom(),
                    texBorder.getLeft(),
                    texBorder.getRight(),
                    zLevel);
                checkGlError("drawContinuousTexturedBox");

            } else if (sliceMode == SliceMode.SLICED_STRETCH) {
                int iu = texBounds.getX() + texBorder.getLeft();
                int iv = texBounds.getY() + texBorder.getTop();
                int iw = texBounds.getWidth() - texBorder.getLeft() - texBorder.getRight();
                int ih = texBounds.getHeight() - texBorder.getTop() - texBorder.getBottom();

                float sx = (iw > 0) ? (float) (w - (texBounds.getWidth() - iw)) / (float) iw : 1F;
                float sy = (ih > 0) ? (float) (h - (texBounds.getHeight() - ih)) / (float) ih : 1F;

                Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
                checkGlError("bindTexture(SLICED_STRETCH)");

                drawCorner(dx, dy, texBounds.getX(), texBounds.getY(), texBorder.getLeft(), texBorder.getTop(), zLevel);
                checkGlError("corner-1");
                
                drawCorner(dx + texBorder.getLeft(), dy, texBounds.getX() + texBorder.getLeft(), texBounds.getY(), iw, texBorder.getTop(), zLevel, sx, 1F);
                checkGlError("corner-2");
                
                drawCorner(dx + w - texBorder.getRight(), dy, texBounds.getX() + texBorder.getLeft() + iw, texBounds.getY(), texBorder.getRight(), texBorder.getTop(), zLevel);
                checkGlError("corner-3");
                
                drawCorner(dx, dy + texBorder.getTop(), texBounds.getX(), texBounds.getY() + texBorder.getTop(), texBorder.getLeft(), ih, zLevel, 1F, sy);
                checkGlError("corner-4");
                
                drawCorner(dx + texBorder.getLeft(), dy + texBorder.getTop(), iu, iv, iw, ih, zLevel, sx, sy);
                checkGlError("corner-5(center)");
                
                drawCorner(dx + w - texBorder.getRight(), dy + texBorder.getTop(), texBounds.getX() + texBorder.getLeft() + iw, texBounds.getY() + texBorder.getTop(), texBorder.getRight(), ih, zLevel, 1F, sy);
                checkGlError("corner-6");
                
                drawCorner(dx, dy + h - texBorder.getBottom(), texBounds.getX(), texBounds.getY() + texBorder.getTop() + ih, texBorder.getLeft(), texBorder.getBottom(), zLevel);
                checkGlError("corner-7");
                
                drawCorner(dx + texBorder.getLeft(), dy + h - texBorder.getBottom(), texBounds.getX() + texBorder.getLeft(), texBounds.getY() + texBorder.getTop() + ih, iw, texBorder.getBottom(), zLevel, sx, 1F);
                checkGlError("corner-8");
                
                drawCorner(dx + w - texBorder.getRight(), dy + h - texBorder.getBottom(), texBounds.getX() + texBorder.getLeft() + iw, texBounds.getY() + texBorder.getTop() + ih, texBorder.getRight(), texBorder.getBottom(), zLevel);
                checkGlError("corner-9");

            } else { // STRETCH
                float sx = (texBounds.getWidth() > 0) ? (float) w / texBounds.getWidth() : 1F;
                float sy = (texBounds.getHeight() > 0) ? (float) h / texBounds.getHeight() : 1F;
                GL11.glTranslatef(dx, dy, 0F);
                checkGlError("glTranslatef(STRETCH)");
                
                GL11.glScalef(sx, sy, 1F);
                checkGlError("glScalef(STRETCH)");
                
                Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
                checkGlError("bindTexture(STRETCH)");
                
                GuiUtils.drawTexturedModalRect(0, 0, texBounds.getX(), texBounds.getY(), texBounds.getWidth(), texBounds.getHeight(), zLevel);
                checkGlError("GuiUtils.drawTexturedModalRect(STRETCH)");
            }

        } finally {
            GL11.glPopMatrix();
            checkGlError("glPopMatrix");
            
            GL11.glPopAttrib();
            checkGlError("glPopAttrib");
            
            // ⚠️ 绝对不要加 while 循环清空错误栈！
        }
    }

    private void applyGlColorSafe(IGuiColor color) {
        if (color == null) {
            GL11.glColor4f(1f, 1f, 1f, 1f);
            return;
        }

        try {
            float r = color.getRed();
            float g = color.getGreen();
            float b = color.getBlue();
            float a = color.getAlpha();

            r = clampColor(r);
            g = clampColor(g);
            b = clampColor(b);
            a = clampColor(a);

            GL11.glColor4f(r, g, b, a);
        } catch (Exception e) {
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private float clampColor(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 1f;
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    private void drawCorner(int x, int y, int u, int v, int w, int h, float z, float sx, float sy) {
        if (w <= 0 || h <= 0) return;
        
        GL11.glPushMatrix();
        checkGlError("corner-pushMatrix");
        
        GL11.glTranslatef(x, y, 0F);
        checkGlError("corner-translate");
        
        if (sx != 1F || sy != 1F) {
            GL11.glScalef(sx, sy, 1F);
            checkGlError("corner-scale");
        }
        
        GuiUtils.drawTexturedModalRect(0, 0, u, v, w, h, z);
        checkGlError("corner-drawModalRect");
        
        GL11.glPopMatrix();
        checkGlError("corner-popMatrix");
    }

    private void drawCorner(int x, int y, int u, int v, int w, int h, float z) {
        drawCorner(x, y, u, v, w, h, z, 1F, 1F);
    }

    @Override
    public ResourceLocation getTexture() {
        return this.texture;
    }

    @Override
    public IGuiRect getBounds() {
        return this.texBounds;
    }

    public GuiPadding getBorder() {
        return this.texBorder;
    }

    public SlicedTexture setSliceMode(SliceMode mode) {
        this.sliceMode = mode;
        return this;
    }

    public static SlicedTexture readFromJson(JsonObject json) {
        ResourceLocation res = new ResourceLocation(JsonHelper.GetString(json, "texture", "minecraft:missingno"));
        int slice = JsonHelper.GetNumber(json, "sliceMode", 1).intValue();
        JsonObject jOut = JsonHelper.GetObject(json, "coordinates");
        int ox = JsonHelper.GetNumber(jOut, "u", 0).intValue();
        int oy = JsonHelper.GetNumber(jOut, "v", 0).intValue();
        int ow = JsonHelper.GetNumber(jOut, "w", 48).intValue();
        int oh = JsonHelper.GetNumber(jOut, "h", 48).intValue();
        JsonObject jIn = JsonHelper.GetObject(json, "border");
        int il = JsonHelper.GetNumber(jIn, "l", 16).intValue();
        int it = JsonHelper.GetNumber(jIn, "t", 16).intValue();
        int ir = JsonHelper.GetNumber(jIn, "r", 16).intValue();
        int ib = JsonHelper.GetNumber(jIn, "b", 16).intValue();
        return new SlicedTexture(res, new GuiRectangle(ox, oy, ow, oh), new GuiPadding(il, it, ir, ib))
            .setSliceMode(SliceMode.values()[slice % 3]);
    }

    private static void drawContinuousTexturedBox(ResourceLocation res, int x, int y, int u, int v, int width,
        int height, int textureWidth, int textureHeight, int topBorder, int bottomBorder, int leftBorder,
        int rightBorder, float zLevel) {

        if (width <= 0 || height <= 0) return;
        if (leftBorder < 0 || rightBorder < 0 || topBorder < 0 || bottomBorder < 0) return;
        if (textureWidth <= 0 || textureHeight <= 0) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(res);

        int fillerWidth = textureWidth - leftBorder - rightBorder;
        int fillerHeight = textureHeight - topBorder - bottomBorder;
        if (fillerWidth <= 0 || fillerHeight <= 0) return;

        int canvasWidth = width - leftBorder - rightBorder;
        int canvasHeight = height - topBorder - bottomBorder;
        int xPasses = canvasWidth / fillerWidth;
        int remainderWidth = canvasWidth % fillerWidth;
        int yPasses = canvasHeight / fillerHeight;
        int remainderHeight = canvasHeight % fillerHeight;

        GuiUtils.drawTexturedModalRect(x, y, u, v, leftBorder, topBorder, zLevel);
        GuiUtils.drawTexturedModalRect(x + leftBorder + canvasWidth, y, u + leftBorder + fillerWidth, v, rightBorder, topBorder, zLevel);
        GuiUtils.drawTexturedModalRect(x, y + topBorder + canvasHeight, u, v + topBorder + fillerHeight, leftBorder, bottomBorder, zLevel);
        GuiUtils.drawTexturedModalRect(x + leftBorder + canvasWidth, y + topBorder + canvasHeight, u + leftBorder + fillerWidth, v + topBorder + fillerHeight, rightBorder, bottomBorder, zLevel);

        for (int i = 0; i < xPasses + (remainderWidth > 0 ? 1 : 0); i++) {
            int drawW = (i == xPasses ? remainderWidth : fillerWidth);
            GuiUtils.drawTexturedModalRect(x + leftBorder + i * fillerWidth, y, u + leftBorder, v, drawW, topBorder, zLevel);
            GuiUtils.drawTexturedModalRect(x + leftBorder + i * fillerWidth, y + topBorder + canvasHeight, u + leftBorder, v + topBorder + fillerHeight, drawW, bottomBorder, zLevel);
            for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
                int drawH = (j == yPasses ? remainderHeight : fillerHeight);
                GuiUtils.drawTexturedModalRect(x + leftBorder + i * fillerWidth, y + topBorder + j * fillerHeight, u + leftBorder, v + topBorder, drawW, drawH, zLevel);
            }
        }

        for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
            int drawH = (j == yPasses ? remainderHeight : fillerHeight);
            GuiUtils.drawTexturedModalRect(x, y + topBorder + j * fillerHeight, u, v + topBorder, leftBorder, drawH, zLevel);
            GuiUtils.drawTexturedModalRect(x + leftBorder + canvasWidth, y + topBorder + j * fillerHeight, u + leftBorder + fillerWidth, v + topBorder, rightBorder, drawH, zLevel);
        }
    }

    public enum SliceMode {
        STRETCH,
        SLICED_TILE,
        SLICED_STRETCH
    }
}
