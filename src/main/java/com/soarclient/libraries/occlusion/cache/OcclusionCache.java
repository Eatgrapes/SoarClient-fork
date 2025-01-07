package com.soarclient.libraries.occlusion.cache;

public interface OcclusionCache {

    void resetCache();

    void setVisible(int x, int y, int z);

    void setHidden(int x, int y, int z);

    int getState(int x, int y, int z);

    void setLastHidden();

    void setLastVisible();

}
