package com.ghostgramlabs.pettibox.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.pettibox.R

enum class KeeperPose {
    Welcome,
    EmptyShelf,
    Search,
    SaveSuccess,
    Reminder,
    NotificationsBlocked,
    Ocr,
    BackupSuccess,
    BackupWarning,
    MissingSave,
    Archive,
    Error
}

@Composable
fun KeeperMascot(
    modifier: Modifier = Modifier,
    size: Dp = 116.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    pose: KeeperPose = KeeperPose.Welcome,
    badgeIcon: ImageVector? = null
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
        )
        Image(
            painter = painterResource(pose.drawableRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size * 0.92f)
        )
        if (badgeIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.30f)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badgeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size * 0.16f)
                )
            }
        }
    }
}

private val KeeperPose.drawableRes: Int
    get() = when (this) {
        KeeperPose.Welcome -> R.drawable.keeper_welcome
        KeeperPose.EmptyShelf -> R.drawable.keeper_empty_shelf
        KeeperPose.Search -> R.drawable.keeper_search
        KeeperPose.SaveSuccess -> R.drawable.keeper_save_success
        KeeperPose.Reminder -> R.drawable.keeper_reminder
        KeeperPose.NotificationsBlocked -> R.drawable.keeper_notifications_blocked
        KeeperPose.Ocr -> R.drawable.keeper_ocr
        KeeperPose.BackupSuccess -> R.drawable.keeper_backup_success
        KeeperPose.BackupWarning -> R.drawable.keeper_backup_warning
        KeeperPose.MissingSave -> R.drawable.keeper_missing_save
        KeeperPose.Archive -> R.drawable.keeper_archive
        KeeperPose.Error -> R.drawable.keeper_error
    }
