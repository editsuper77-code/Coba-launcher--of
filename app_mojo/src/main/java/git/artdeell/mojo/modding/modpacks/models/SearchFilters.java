package git.artdeell.mojo.modding.modpacks.models;

import org.jetbrains.annotations.Nullable;

/**
 * Search filters, passed to APIs
 */
public class SearchFilters {
    public boolean isModpack;
    /** One of modpack, mod, shader, resourcepack */
    public String projectType = "modpack";
    public String name;
    @Nullable public String mcVersion;

}
