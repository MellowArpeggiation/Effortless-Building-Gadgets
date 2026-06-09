package net.mellow.effortless.buildmode;

import java.util.Arrays;
import java.util.Locale;

import net.mellow.effortless.buildmode.modes.*;

public class ModeOptions {
    
    public static enum BuildingMode {
        NONE(null, 0, 16, 0xd4de3b), // disabled
        EXTENDED(new Extended(), 16, 16, 0xd4de3b), // greater reach
        AIR(new Air(), 240, 16, 0xd4de3b), // air placement
        LINE(new Line(), 32, 16, 0x0080ff), // lines
        WALL(new Wall(), 48, 16, 0x0080ff, BuildingOption.FILL), // walls
        FLOOR(new Floor(), 64, 16, 0x0080ff, BuildingOption.FILL), // floors
        CUBE(new Cube(), 80, 16, 0x0080ff, BuildingOption.CUBE_FILL), // miney crafta
        DIAGONAL_LINE(new DiagonalLine(), 96, 16, 0x8f47de), // okay I think you get it now
        DIAGONAL_WALL(new DiagonalWall(), 112, 16, 0x8f47de, BuildingOption.FILL),
        SLOPE_FLOOR(new SlopeFloor(), 128, 16, 0x8f47de, BuildingOption.RAISED_EDGE),
        CIRCLE(new Circle(), 144, 16, 0x4ac24d, BuildingOption.CIRCLE_START, BuildingOption.FILL);

        public final BaseBuildMode handler;
        public final int iconX;
        public final int iconY;
        public final BuildingOption[] options;
        public final int r;
        public final int g;
        public final int b;

        private BuildingMode(BaseBuildMode handler, int iconX, int iconY, int color, BuildingOption... options) {
            this.handler = handler;
            this.iconX = iconX;
            this.iconY = iconY;
            this.options = options;

            this.r = color >> 16;
            this.g = color >> 8 & 0xFF;
            this.b = color & 0xFF;
        }
        
        public String getUnlocalizedName() {
            return "buildingmode." + name().toLowerCase(Locale.ROOT) + ".name";
        }

        public String getUnlocalizedDesc() {
            return "buildingmode." + name().toLowerCase(Locale.ROOT) + ".desc";
        }

        public static BuildingMode[] getRegularModes() {
            BuildingMode[] modes = BuildingMode.values();
            return Arrays.copyOfRange(modes, 1, modes.length);
        }

        public static BuildingMode[] getItemlessModes() {
            return BuildingMode.values();
        }
    }

    public static enum BuildingAction {
        UNDO(16, 0),
        REDO(32, 0),

        FULL(32, 32),
        HOLLOW(48, 32),

        CUBE_FULL(64, 32),
        CUBE_HOLLOW(80, 32),
        CUBE_SKELETON(96, 32),

        SHORT_EDGE(112, 32),
        LONG_EDGE(128, 32),

        CIRCLE_START_CORNER(144, 32),
        CIRCLE_START_CENTER(160, 32);

        public final int iconX;
        public final int iconY;

        private BuildingAction(int iconX, int iconY) {
            this.iconX = iconX;
            this.iconY = iconY;
        }

        public String getUnlocalizedName() {
            return "buildingaction." + name().toLowerCase(Locale.ROOT) + ".name";
        }

        public String getUnlocalizedDesc() {
            return "buildingaction." + name().toLowerCase(Locale.ROOT) + ".desc";
        }

        public static BuildingAction[] getGlobalActions() {
            return new BuildingAction[] {
                UNDO,
                REDO,
            };
        }
    }

    public static enum BuildingOption {
        FILL(BuildingAction.FULL, BuildingAction.HOLLOW),
        CUBE_FILL(BuildingAction.CUBE_FULL, BuildingAction.CUBE_HOLLOW, BuildingAction.CUBE_SKELETON),
        RAISED_EDGE(BuildingAction.SHORT_EDGE, BuildingAction.LONG_EDGE),
        CIRCLE_START(BuildingAction.CIRCLE_START_CORNER, BuildingAction.CIRCLE_START_CENTER);

        public final BuildingAction[] actions;

        private BuildingOption(BuildingAction... actions) {
            this.actions = actions;
        }
        
        public String getUnlocalizedName() {
            return "buildingoption." + name().toLowerCase(Locale.ROOT) + ".name";
        }

        public String getUnlocalizedDesc() {
            return "buildingoption." + name().toLowerCase(Locale.ROOT) + ".desc";
        }
    }
    
}
