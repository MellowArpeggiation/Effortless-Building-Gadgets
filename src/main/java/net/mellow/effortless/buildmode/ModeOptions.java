package net.mellow.effortless.buildmode;

import java.util.Locale;

import net.mellow.effortless.buildmode.modes.*;

public class ModeOptions {
    
    public static enum BuildingMode {
        EXTENDED(new Extended(), 16, 16), // greater reach
        AIR(new Air(), 240, 16), // air placement
        LINE(new Line(), 32, 16), // lines
        WALL(new Wall(), 48, 16, BuildingOption.FILL), // walls
        FLOOR(new Floor(), 64, 16, BuildingOption.FILL), // floors
        CUBE(new Cube(), 80, 16); // miney crafta

        public final BaseBuildMode handler;
        public final int iconX;
        public final int iconY;
        public final BuildingOption[] options;

        private BuildingMode(BaseBuildMode handler, int iconX, int iconY, BuildingOption... options) {
            this.handler = handler;
            this.iconX = iconX;
            this.iconY = iconY;
            this.options = options;
        }
        
        public String getUnlocalizedName() {
            return "buildingmode." + name().toLowerCase(Locale.ROOT) + ".name";
        }

        public String getUnlocalizedDesc() {
            return "buildingmode." + name().toLowerCase(Locale.ROOT) + ".desc";
        }
    }

    public static enum BuildingAction {
        UNDO(16, 0),
        REDO(32, 0),

        FULL(32, 32),
        HOLLOW(48, 32);

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
        FILL(BuildingAction.FULL, BuildingAction.HOLLOW);

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
