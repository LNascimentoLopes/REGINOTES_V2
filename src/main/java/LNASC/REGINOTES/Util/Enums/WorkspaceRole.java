package LNASC.REGINOTES.Util.Enums;

import lombok.Getter;

@Getter
public enum WorkspaceRole {
    OWNER(4),
    ADMIN(3),
    EDITOR(2),
    VIEWER(1);

    private final int level;

    WorkspaceRole(int level){
        this.level = level;
    }
}
