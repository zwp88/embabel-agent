package com.embabel.agent.tools.math

import com.embabel.agent.api.common.SelfToolGroup
import com.embabel.agent.common.Constants
import com.embabel.agent.core.CoreToolGroups.MATH_DESCRIPTION
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.common.core.types.Semver

class MathTools : SelfToolGroup {

    override val description: ToolGroupDescription = MATH_DESCRIPTION

    override val provider: String = Constants.EMBABEL_PROVIDER
    override val version = Semver(0, 1, 0)
    override val permissions: Set<ToolGroupPermission>
        get() = emptySet()
}