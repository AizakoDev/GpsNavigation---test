package com.sergeyapp.gpsnavigation_test.presenetation.info

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sergeyapp.gpsnavigation_test.presenetation.info.model.UserMessageModel

// Тост отображения информации для юзера
fun AppCompatActivity.showUserInfo(userMessageModel: UserMessageModel) {
    Toast.makeText(this, userMessageModel.messageForUser, Toast.LENGTH_SHORT).show()
}