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
        this.watermark = null;
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

}
