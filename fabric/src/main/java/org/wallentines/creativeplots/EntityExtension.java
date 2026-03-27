package org.wallentines.creativeplots;

import org.jetbrains.annotations.Nullable;
import org.wallentines.midnightlib.math.Vec2i;

public interface EntityExtension {

    @Nullable
    Vec2i getHomePlot();

    void setHomePlot(@Nullable Vec2i plot);

}
