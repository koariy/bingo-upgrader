package dev.bingoupgrader.common.bingo;
/** Shared JSON transport/model serialization; Minecraft already supplies Gson. */
public final class BingoJson {
    public static final com.google.gson.Gson JSON=new com.google.gson.GsonBuilder().disableHtmlEscaping().create();
    private BingoJson() { }
}
