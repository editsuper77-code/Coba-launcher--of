package git.artdeell.mojo.modding.modpacks.models;

public abstract class ModSource {
    public int apiSource;
    public boolean isModpack;
    /** One of modpack, mod, shader, resourcepack */
    public String projectType = "modpack";
}
