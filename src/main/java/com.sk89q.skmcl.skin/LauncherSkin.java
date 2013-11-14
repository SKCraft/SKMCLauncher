/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010, 2011 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.skmcl.skin;

import org.pushingpixels.substance.api.*;
import org.pushingpixels.substance.api.colorscheme.EbonyColorScheme;
import org.pushingpixels.substance.api.painter.border.ClassicBorderPainter;
import org.pushingpixels.substance.api.painter.border.CompositeBorderPainter;
import org.pushingpixels.substance.api.painter.border.DelegateBorderPainter;
import org.pushingpixels.substance.api.painter.decoration.FlatDecorationPainter;
import org.pushingpixels.substance.api.painter.fill.FractionBasedFillPainter;
import org.pushingpixels.substance.api.painter.highlight.ClassicHighlightPainter;
import org.pushingpixels.substance.api.skin.GraphiteSkin;
import org.pushingpixels.substance.api.watermark.SubstanceCrosshatchWatermark;
import org.pushingpixels.substance.api.watermark.SubstanceWatermark;
import org.pushingpixels.substance.internal.utils.SubstanceCoreUtilities;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LauncherSkin extends GraphiteSkin {

    public LauncherSkin() {
        SubstanceSkin.ColorSchemes schemes = SubstanceSkin
                .getColorSchemes("com/sk89q/skmcl/skin/graphite.colorschemes");

        SubstanceColorScheme activeScheme = schemes.get("Graphite Active");
        SubstanceColorScheme selectedDisabledScheme = schemes
                .get("Graphite Selected Disabled");
        SubstanceColorScheme selectedScheme = schemes.get("Graphite Selected");
        SubstanceColorScheme disabledScheme = schemes.get("Graphite Disabled");

        SubstanceColorScheme enabledScheme = schemes.get("Graphite Enabled");
        SubstanceColorScheme backgroundScheme = schemes
                .get("Graphite Background");

        SubstanceColorSchemeBundle defaultSchemeBundle = new SubstanceColorSchemeBundle(
                activeScheme, enabledScheme, disabledScheme);

        // highlight fill scheme + custom alpha for rollover unselected state
        SubstanceColorScheme highlightScheme = schemes
                .get("Graphite Highlight");
        defaultSchemeBundle.registerHighlightColorScheme(highlightScheme, 0.6f,
                ComponentState.ROLLOVER_UNSELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(highlightScheme, 0.8f,
                ComponentState.SELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(highlightScheme, 1.0f,
                ComponentState.ROLLOVER_SELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(highlightScheme,
                0.75f, ComponentState.ARMED, ComponentState.ROLLOVER_ARMED);

        // highlight border scheme
        SubstanceColorScheme borderScheme = schemes.get("Graphite Border");
        SubstanceColorScheme separatorScheme = schemes
                .get("Graphite Separator");
        defaultSchemeBundle.registerColorScheme(new EbonyColorScheme(),
                ColorSchemeAssociationKind.HIGHLIGHT_BORDER, ComponentState
                .getActiveStates());
        defaultSchemeBundle.registerColorScheme(borderScheme,
                ColorSchemeAssociationKind.BORDER);
        defaultSchemeBundle.registerColorScheme(separatorScheme,
                ColorSchemeAssociationKind.SEPARATOR);

        // text highlight scheme
        SubstanceColorScheme textHighlightScheme = schemes
                .get("Graphite Text Highlight");
        defaultSchemeBundle.registerColorScheme(textHighlightScheme,
                ColorSchemeAssociationKind.TEXT_HIGHLIGHT,
                ComponentState.SELECTED, ComponentState.ROLLOVER_SELECTED);

        defaultSchemeBundle.registerColorScheme(highlightScheme,
                ComponentState.ARMED, ComponentState.ROLLOVER_ARMED);

        SubstanceColorScheme highlightMarkScheme = schemes
                .get("Graphite Highlight Mark");
        defaultSchemeBundle.registerColorScheme(highlightMarkScheme,
                ColorSchemeAssociationKind.HIGHLIGHT_MARK, ComponentState
                .getActiveStates());
        defaultSchemeBundle.registerColorScheme(highlightMarkScheme,
                ColorSchemeAssociationKind.MARK,
                ComponentState.ROLLOVER_SELECTED,
                ComponentState.ROLLOVER_UNSELECTED);
        defaultSchemeBundle.registerColorScheme(borderScheme,
                ColorSchemeAssociationKind.MARK, ComponentState.SELECTED);

        defaultSchemeBundle.registerColorScheme(disabledScheme, 0.5f,
                ComponentState.DISABLED_UNSELECTED);
        defaultSchemeBundle.registerColorScheme(selectedDisabledScheme, 0.65f,
                ComponentState.DISABLED_SELECTED);

        defaultSchemeBundle.registerColorScheme(highlightScheme,
                ComponentState.ROLLOVER_SELECTED);
        defaultSchemeBundle.registerColorScheme(selectedScheme,
                ComponentState.SELECTED);

        SubstanceColorScheme tabHighlightScheme = schemes
                .get("Graphite Tab Highlight");
        defaultSchemeBundle.registerColorScheme(tabHighlightScheme,
                ColorSchemeAssociationKind.TAB,
                ComponentState.ROLLOVER_SELECTED);

        this.registerDecorationAreaSchemeBundle(defaultSchemeBundle,
                backgroundScheme, DecorationAreaType.NONE);

        this.setSelectedTabFadeStart(0.1);
        this.setSelectedTabFadeEnd(0.3);

        this.buttonShaper = new LauncherButtonShaper();
        this.watermark = new TextureWatermark();
        this.fillPainter = new FractionBasedFillPainter("Graphite",
                new float[] { 0.0f, 0.5f, 1.0f },
                new ColorSchemeSingleColorQuery[] {
                        ColorSchemeSingleColorQuery.ULTRALIGHT,
                        ColorSchemeSingleColorQuery.LIGHT,
                        ColorSchemeSingleColorQuery.LIGHT });
        this.decorationPainter = new FlatDecorationPainter();
        this.highlightPainter = new ClassicHighlightPainter();
        this.borderPainter = new CompositeBorderPainter("Graphite",
                new ClassicBorderPainter(), new DelegateBorderPainter(
                "Graphite Inner", new ClassicBorderPainter(),
                0xA0FFFFFF, 0x60FFFFFF, 0x60FFFFFF,
                new ColorSchemeTransform() {
                    @Override
                    public SubstanceColorScheme transform(
                            SubstanceColorScheme scheme) {
                        return scheme.tint(0.25f);
                    }
                }));

        this.highlightBorderPainter = new ClassicBorderPainter();

        this.watermarkScheme = schemes.get("Graphite Watermark");
    }


    private static class TextureWatermark implements SubstanceWatermark {
        /**
         * Watermark image (screen-sized).
         */
        private static Image watermarkImage = null;

        /*
         * (non-Javadoc)
         *
         * @seeorg.pushingpixels.substance.watermark.SubstanceWatermark#
         * drawWatermarkImage(java .awt.Graphics, int, int, int, int)
         */
        @Override
        public void drawWatermarkImage(Graphics graphics, Component c, int x,
                                       int y, int width, int height) {
            if (!c.isShowing())
                return;
            int dx = c.getLocationOnScreen().x;
            int dy = c.getLocationOnScreen().y;
            graphics.drawImage(TextureWatermark.watermarkImage, x, y,
                    x + width, y + height, x + dx, y + dy, x + dx + width, y
                    + dy + height, null);
        }

        /*
         * (non-Javadoc)
         *
         * @seeorg.pushingpixels.substance.watermark.SubstanceWatermark#
         * updateWatermarkImage (org.pushingpixels.substance.skin.SubstanceSkin)
         */
        @Override
        public boolean updateWatermarkImage(SubstanceSkin skin) {
            // fix by Chris for bug 67 - support for multiple screens
            Rectangle virtualBounds = new Rectangle();
            GraphicsEnvironment ge = GraphicsEnvironment
                    .getLocalGraphicsEnvironment();
            GraphicsDevice[] gds = ge.getScreenDevices();
            for (GraphicsDevice gd : gds) {
                GraphicsConfiguration gc = gd.getDefaultConfiguration();
                virtualBounds = virtualBounds.union(gc.getBounds());
            }

            int screenWidth = virtualBounds.width;
            int screenHeight = virtualBounds.height;
            TextureWatermark.watermarkImage = SubstanceCoreUtilities
                    .getBlankImage(screenWidth, screenHeight);

            Graphics2D graphics = (Graphics2D) TextureWatermark.watermarkImage
                    .getGraphics().create();
            boolean status = this.drawWatermarkImage(skin, graphics, 0, 0,
                    screenWidth, screenHeight, false);
            graphics.dispose();
            return status;
        }

        /*
         * (non-Javadoc)
         *
         * @seeorg.pushingpixels.substance.api.watermark.SubstanceWatermark#
         * previewWatermark (java.awt.Graphics,
         * org.pushingpixels.substance.api.SubstanceSkin, int, int, int, int)
         */
        @Override
        public void previewWatermark(Graphics g, SubstanceSkin skin, int x,
                                     int y, int width, int height) {
            this.drawWatermarkImage(skin, (Graphics2D) g, x, y, width, height,
                    true);
        }

        /**
         * Draws the specified portion of the watermark image.
         *
         * @param skin
         *            Skin to use for painting the watermark.
         * @param graphics
         *            Graphic context.
         * @param x
         *            the <i>x</i> coordinate of the watermark to be drawn.
         * @param y
         *            The <i>y</i> coordinate of the watermark to be drawn.
         * @param width
         *            The width of the watermark to be drawn.
         * @param height
         *            The height of the watermark to be drawn.
         * @param isPreview
         *            Indication whether the result is a preview image.
         * @return Indication whether the draw succeeded.
         */
        private boolean drawWatermarkImage(SubstanceSkin skin,
                                           Graphics2D graphics, int x, int y, int width, int height,
                                           boolean isPreview) {
            Color stampColorDark;
            Color stampColorAll;
            //Color stampColorLight = null;
            SubstanceColorScheme scheme = skin.getWatermarkColorScheme();
            if (isPreview) {
                stampColorDark = scheme.isDark() ? Color.white : Color.black;
                stampColorAll = Color.lightGray;
                //stampColorLight = scheme.isDark() ? Color.black : Color.white;
            } else {
                stampColorDark = scheme.getWatermarkDarkColor();
                stampColorAll = scheme.getWatermarkStampColor();
                //stampColorLight = scheme.getWatermarkLightColor();
            }

            graphics.setColor(stampColorAll);
            graphics.fillRect(0, 0, width, height);

            BufferedImage tile = SubstanceCoreUtilities.getBlankImage(8, 4);
            int rgbDark = stampColorDark.getRGB();
            tile.setRGB(0, 0, rgbDark);
            tile.setRGB(0, 1, rgbDark);
            tile.setRGB(0, 2, rgbDark);
            tile.setRGB(0, 3, rgbDark);
            tile.setRGB(1, 2, rgbDark);
            tile.setRGB(2, 1, rgbDark);
            tile.setRGB(3, 0, rgbDark);
            tile.setRGB(4, 0, rgbDark);
            tile.setRGB(4, 1, rgbDark);
            tile.setRGB(4, 2, rgbDark);
            tile.setRGB(4, 3, rgbDark);
            tile.setRGB(5, 0, rgbDark);
            tile.setRGB(6, 1, rgbDark);
            tile.setRGB(7, 2, rgbDark);

            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.05f));
            for (int row = y; row < (y + height); row += 4) {
                for (int col = x; col < (x + width); col += 8) {
                    g2d.drawImage(tile, col, row, null);
                }
            }
            g2d.dispose();
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.pushingpixels.substance.api.trait.SubstanceTrait#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return SubstanceCrosshatchWatermark.getName();
        }

        /**
         * Returns the name of all watermarks of <code>this</code> class.
         *
         * @return The name of all watermarks of <code>this</code> class.
         */
        public static String getName() {
            return "Crosshatch";
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.pushingpixels.substance.watermark.SubstanceWatermark#dispose()
         */
        @Override
        public void dispose() {
            watermarkImage = null;
        }
    }
}
