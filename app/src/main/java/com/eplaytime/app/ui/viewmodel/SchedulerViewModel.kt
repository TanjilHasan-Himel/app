package com.eplaytime.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eplaytime.app.data.dao.ScheduledTaskDao
import com.eplaytime.app.data.model.ScheduledTask
import com.eplaytime.app.util.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SchedulerViewModel - Manages scheduled playback tasks
 */
@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val taskDao: ScheduledTaskDao,
    @ApplicationContext context: Context
) : ViewModel() {

    private val alarmScheduler = AlarmScheduler(context)

    private val _tasks = MutableStateFlow<List<ScheduledTask>>(emptyList())
    val tasks: StateFlow<List<ScheduledTask>> = _tasks.asStateFlow()

    private val _selectedHour = MutableStateFlow(8)
    val selectedHour: StateFlow<Int> = _selectedHour.asStateFlow()

    private val _selectedMinute = MutableStateFlow(0)
    val selectedMinute: StateFlow<Int> = _selectedMinute.asStateFlow()

    init {
        loadTasks()
    }

    /**
     * Load all scheduled tasks
     */
    private fun loadTasks() {
        viewModelScope.launch {
            taskDao.getAllTasks().collect { taskList ->
                _tasks.value = taskList
            }
        }
    }

    /**
     * Create a new scheduled task
     */
    fun createTask(
        contentType: String,
        contentUris: List<String>,
        contentTitle: String,
        hour: Int,
        minute: Int,
        targetVolume: Float,
        repeatDays: String,
        stopAfterMinutes: Int,
        fadeInSeconds: Int
    ) {
        viewModelScope.launch {
            val firstUri = contentUris.firstOrNull().orEmpty()
            val task = ScheduledTask(
                songUri = firstUri,
                songTitle = contentTitle,
                hour = hour,
                minute = minute,
                isEnabled = true,
                targetVolume = targetVolume,
                repeatDays = repeatDays,
                contentType = contentType,
                contentUris = contentUris.joinToString("\n"),
                stopAfterMinutes = stopAfterMinutes,
                fadeInSeconds = fadeInSeconds
            )

            val taskId = taskDao.insertTask(task)
            val insertedTask = task.copy(id = taskId)

            // Schedule the alarm
            persistScheduleResult(insertedTask, alarmScheduler.scheduleAlarm(insertedTask))
        }
    }

    /**
     * Delete a task
     */
    fun deleteTask(task: ScheduledTask) {
        viewModelScope.launch {
            // Cancel alarm first
            alarmScheduler.cancelAlarm(task)
            // Then delete from database
            taskDao.deleteTask(task)
        }
    }

    /**
     * Toggle task enabled/disabled
     */
    fun toggleTask(task: ScheduledTask) {
        viewModelScope.launch {
            val updatedTask = task.copy(isEnabled = !task.isEnabled)
            taskDao.updateTask(updatedTask)

            if (updatedTask.isEnabled) {
                persistScheduleResult(updatedTask, alarmScheduler.scheduleAlarm(updatedTask))
            } else {
                alarmScheduler.cancelAlarm(updatedTask)
            }
        }
    }

    /**
     * Update an existing task
     */
    fun updateTask(
        task: ScheduledTask,
        contentType: String,
        contentUris: List<String>,
        contentTitle: String,
        hour: Int,
        minute: Int,
        targetVolume: Float,
        repeatDays: String,
        stopAfterMinutes: Int,
        fadeInSeconds: Int
    ) {
        viewModelScope.launch {
            // Cancel old alarm
            alarmScheduler.cancelAlarm(task)
            
            // Create updated task object
            val firstUri = contentUris.firstOrNull().orEmpty()
            val updatedTask = task.copy(
                songUri = firstUri,
                songTitle = contentTitle,
                hour = hour,
                minute = minute,
                isEnabled = true, // Re-enable on edit? Usually yes.
                targetVolume = targetVolume,
                repeatDays = repeatDays,
                contentType = contentType,
                contentUris = contentUris.joinToString("\n"),
                stopAfterMinutes = stopAfterMinutes,
                fadeInSeconds = fadeInSeconds
            )

            // Update DB
            taskDao.updateTask(updatedTask)

            // Schedule new alarm
            persistScheduleResult(updatedTask, alarmScheduler.scheduleAlarm(updatedTask))
        }
    }

    private suspend fun persistScheduleResult(task: ScheduledTask, nextRunAt: Long?) {
        if (nextRunAt == null) {
            taskDao.updateRunState(task.id, task.lastRunAt, "NEEDS_PERMISSION", "", 0, 0)
        } else {
            taskDao.updateSchedulingState(task.id, nextRunAt, "READY")
        }
    }

    /**
     * Update selected time
     */
    fun updateTime(hour: Int, minute: Int) {
        _selectedHour.value = hour
        _selectedMinute.value = minute
    }
}
