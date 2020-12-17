package de.cowtipper.cowlection.config.gui;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.MooConfigCategory;
import de.cowtipper.cowlection.listener.PlayerListener;
import de.cowtipper.cowlection.util.GuiHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Arrays;

/**
 * Main config gui containing:
 * <ul>
 * <li>menu ({@link MooConfigMenuList}) with list of config categories ({@link MooConfigCategory})</li>
 * <li>the current opened config category with its sub-categories ({@link MooConfigCategoryScrolling})</li>
 * </ul>
 * Based on {@link net.minecraft.client.gui.GuiControls}
 */
public class MooConfigGui extends GuiScreen {
    public static long showDungeonPerformanceOverlayUntil;
    public int menuWidth;
    private MooConfigMenuList menu;
    private int selectedMenuIndex = -1;
    private MooConfigCategory currentConfigCategory;
    private final boolean isOutsideOfSkyBlock;
    /**
     * equivalent of GuiModList.keyBindingList
     */
    private MooConfigCategoryScrolling currentConfigCategoryGui;
    private GuiButton btnClose;

    public MooConfigGui() {
        isOutsideOfSkyBlock = PlayerListener.registerSkyBlockListeners();
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        MooConfigPreview.parent = this;

        // re-register SkyBlock listeners if necessary (mainly so that previews function correctly outside of SkyBlock)
        PlayerListener.registerSkyBlockListeners();

        for (MooConfigCategory configCategory : MooConfig.getConfigCategories()) {
            menuWidth = Math.max(menuWidth, fontRendererObj.getStringWidth(configCategory.getMenuDisplayName()) + 10 + 2);
        }
        menuWidth = Math.min(menuWidth, 150);
        this.menu = new MooConfigMenuList(this, menuWidth);

        this.buttonList.add(this.btnClose = new GuiButton(6, this.width - 25, 3, 22, 20, EnumChatFormatting.RED + "X"));

        if (selectedMenuIndex < 0) {
            // switch to 1st category if none is selected
            selectConfigCategory(0);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (currentConfigCategoryGui != null) {
            this.currentConfigCategoryGui.handleMouseInput();
        }
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled) {
            if (button.id == 6) { // close gui
                this.mc.displayGuiScreen(null);
            }
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0 || (currentConfigCategoryGui != null && !this.currentConfigCategoryGui.mouseClicked(mouseX, mouseY, mouseButton))) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /**
     * Called when a mouse button is released.  Args : mouseX, mouseY, releaseButton
     */
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state != 0 || currentConfigCategoryGui != null && !this.currentConfigCategoryGui.mouseReleased(mouseX, mouseY, state)) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && (currentConfigCategoryGui == null || !currentConfigCategoryGui.isModifyingKeyBind())) {
            super.keyTyped(typedChar, keyCode);
        } else if (this.currentConfigCategoryGui != null) {
            this.currentConfigCategoryGui.keyTyped(typedChar, keyCode);
        }
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!showDungeonPerformanceOverlay()) {
            this.menu.drawScreen(mouseX, mouseY, partialTicks);
        }

        String guiTitle = "" + EnumChatFormatting.BOLD + EnumChatFormatting.UNDERLINE + Cowlection.MODNAME + " config" + (currentConfigCategory != null ? ":" + EnumChatFormatting.RESET + " " + currentConfigCategory.getDisplayName() : "");
        int guiTitleX = ((menu.getRight() + this.width) / 2) - this.fontRendererObj.getStringWidth(guiTitle) / 2;
        this.drawCenteredString(this.fontRendererObj, guiTitle, guiTitleX, 16, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (currentConfigCategoryGui != null) {
            currentConfigCategoryGui.drawScreen(mouseX, mouseY, partialTicks);
        }
        if (btnClose.isMouseOver()) {
            GuiHelper.drawHoveringText(Arrays.asList(EnumChatFormatting.RED + "Save & close settings", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET + " alternatively press ESC"), mouseX, mouseY, width, height, 300);
        }
    }

    @Override
    public void drawDefaultBackground() {
        if (!MooConfigGui.showDungeonPerformanceOverlay()) {
            super.drawDefaultBackground();
        }
    }

    // config category menu methods:

    /**
     * Select a config category via the menu
     */
    public void selectConfigCategory(int index) {
        if (index == this.selectedMenuIndex) {
            return;
        }
        this.selectedMenuIndex = index;
        this.currentConfigCategory = (index >= 0 && index <= MooConfig.getConfigCategories().size()) ? MooConfig.getConfigCategories().get(selectedMenuIndex) : null;

        switchDisplayedConfigCategory();
        Cowlection.getInstance().getConfig().syncFromGui();
    }

    /**
     * Helper method for menu: is config category selected?
     */
    public boolean isConfigCategorySelected(int index) {
        return index == selectedMenuIndex;
    }

    public void switchDisplayedConfigCategory() {
        if (currentConfigCategory == null) {
            return;
        }
        currentConfigCategoryGui = new MooConfigCategoryScrolling(this, mc, currentConfigCategory, menu.getRight() + 3);
    }

    @Override
    public void renderToolTip(ItemStack stack, int x, int y) {
        super.renderToolTip(stack, x, y);
    }

    @Override
    public void handleComponentHover(IChatComponent component, int x, int y) {
        super.handleComponentHover(component, x, y);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        Cowlection.getInstance().getConfig().syncFromGui();

        if (isOutsideOfSkyBlock) {
            PlayerListener.unregisterSkyBlockListeners();
        }
        if (MooConfigPreview.parent != null) {
            MooConfigPreview.parent = null;
        }
    }

    public static boolean showDungeonPerformanceOverlay() {
        return showDungeonPerformanceOverlayUntil > System.currentTimeMillis();
    }
}