package com.example.testapp004.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.RelationTypes
import kotlinx.coroutines.launch

abstract class CanvasViewModel(
    private val relationRepository: RelationRepository,
) : ViewModel() {

    protected abstract val dialogFromId: Long?
    protected abstract val dialogToId: Long?
    protected abstract fun nodesSnapshot(): List<CanvasPersonNode>
    protected abstract fun applyDialogState(
        isOpen: Boolean,
        fromId: Long?,
        toId: Long?,
        fromName: String,
        toName: String,
    )

    fun openRelationDialog(fromId: Long, toId: Long) {
        val nodes = nodesSnapshot()
        applyDialogState(
            isOpen = true,
            fromId = fromId,
            toId = toId,
            fromName = nodes.find { it.id == fromId }?.name ?: "",
            toName = nodes.find { it.id == toId }?.name ?: "",
        )
    }

    fun closeRelationDialog() {
        applyDialogState(isOpen = false, fromId = null, toId = null, fromName = "", toName = "")
    }

    fun addRelationFromCanvas(typeKey: String, isDragSourceFrom: Boolean, customLabel: String?) {
        val fromId = dialogFromId ?: return
        val toId = dialogToId ?: return
        if (typeKey == RelationTypes.CUSTOM_KEY && customLabel.isNullOrBlank()) return
        val actualFromId = if (isDragSourceFrom) fromId else toId
        val actualToId = if (isDragSourceFrom) toId else fromId
        viewModelScope.launch {
            relationRepository.addRelation(actualFromId, actualToId, typeKey, customLabel)
            closeRelationDialog()
        }
    }
}
