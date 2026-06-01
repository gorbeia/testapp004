package com.example.testapp004.model

enum class RelationCategory { FAMILY, PROFESSIONAL, SOCIAL }

data class RelationType(
    val key: String,
    val fromLabel: String,
    val toLabel: String,
    val category: RelationCategory,
) {
    val isSymmetric: Boolean get() = fromLabel == toLabel
}

object RelationTypes {
    val SPOUSE = RelationType("SPOUSE", "Spouse", "Spouse", RelationCategory.FAMILY)
    val PARTNER = RelationType("PARTNER", "Partner", "Partner", RelationCategory.FAMILY)
    val PARENT_CHILD = RelationType("PARENT_CHILD", "Parent", "Child", RelationCategory.FAMILY)
    val SIBLING = RelationType("SIBLING", "Sibling", "Sibling", RelationCategory.FAMILY)
    val GRANDPARENT_GRANDCHILD = RelationType("GRANDPARENT_GRANDCHILD", "Grandparent", "Grandchild", RelationCategory.FAMILY)
    val UNCLE_AUNT = RelationType("UNCLE_AUNT", "Uncle / Aunt", "Nephew / Niece", RelationCategory.FAMILY)
    val COUSIN = RelationType("COUSIN", "Cousin", "Cousin", RelationCategory.FAMILY)
    val STEP_PARENT_CHILD = RelationType("STEP_PARENT_CHILD", "Step-parent", "Step-child", RelationCategory.FAMILY)
    val GUARDIAN = RelationType("GUARDIAN", "Guardian", "Ward", RelationCategory.FAMILY)

    val MANAGER_REPORT = RelationType("MANAGER_REPORT", "Manager", "Direct report", RelationCategory.PROFESSIONAL)
    val MENTOR_MENTEE = RelationType("MENTOR_MENTEE", "Mentor", "Mentee", RelationCategory.PROFESSIONAL)
    val COLLEAGUE = RelationType("COLLEAGUE", "Colleague", "Colleague", RelationCategory.PROFESSIONAL)
    val EMPLOYER_EMPLOYEE = RelationType("EMPLOYER_EMPLOYEE", "Employer", "Employee", RelationCategory.PROFESSIONAL)

    val FRIEND = RelationType("FRIEND", "Friend", "Friend", RelationCategory.SOCIAL)
    val NEIGHBOR = RelationType("NEIGHBOR", "Neighbor", "Neighbor", RelationCategory.SOCIAL)
    val ROOMMATE = RelationType("ROOMMATE", "Roommate", "Roommate", RelationCategory.SOCIAL)
    val CLASSMATE = RelationType("CLASSMATE", "Classmate", "Classmate", RelationCategory.SOCIAL)

    const val CUSTOM_KEY = "CUSTOM"

    private val all = listOf(
        SPOUSE, PARTNER, PARENT_CHILD, SIBLING, GRANDPARENT_GRANDCHILD, UNCLE_AUNT,
        COUSIN, STEP_PARENT_CHILD, GUARDIAN,
        MANAGER_REPORT, MENTOR_MENTEE, COLLEAGUE, EMPLOYER_EMPLOYEE,
        FRIEND, NEIGHBOR, ROOMMATE, CLASSMATE,
    )

    fun findByKey(key: String): RelationType? = all.find { it.key == key }

    fun typeOptions(): List<RelationTypeOption> = buildList {
        for (type in all) {
            add(RelationTypeOption(type.fromLabel, type.key, isCurrentPersonFrom = true, type.category))
            if (!type.isSymmetric) {
                add(RelationTypeOption(type.toLabel, type.key, isCurrentPersonFrom = false, type.category))
            }
        }
        add(RelationTypeOption("Custom…", CUSTOM_KEY, isCurrentPersonFrom = true, RelationCategory.SOCIAL))
    }
}

data class RelationTypeOption(
    val displayLabel: String,
    val typeKey: String,
    val isCurrentPersonFrom: Boolean,
    val category: RelationCategory,
)
