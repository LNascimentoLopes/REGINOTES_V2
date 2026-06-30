package LNASC.REGINOTES.Util.Enums;

import lombok.Getter;

@Getter
public enum NoteRole {
    OWNER(3),
    EDITOR(2),
    VIEWER(1);

    private final int level;

    NoteRole(int level){
        this.level = level;
    }
}
