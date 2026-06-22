/* ktlint-disable */
@file:Suppress("SwallowedException", "MagicNumber")

package com.roxybasicneedbot.kdownloader.desktop.ui

import java.awt.AWTException
import java.awt.Color
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

object SystemTrayManager {
    private var trayIcon: TrayIcon? = null
    var onExitClicked: (() -> Unit)? = null
    var onPauseAllClicked: (() -> Unit)? = null
    var onResumeAllClicked: (() -> Unit)? = null

    fun initialize() {
        if (!SystemTray.isSupported()) {
            println("SystemTray is not supported")
            return
        }

        val popup = PopupMenu()
        
        val pauseAllItem = MenuItem("Pause All")
        pauseAllItem.addActionListener { onPauseAllClicked?.invoke() }
        
        val resumeAllItem = MenuItem("Resume All")
        resumeAllItem.addActionListener { onResumeAllClicked?.invoke() }
        
        val exitItem = MenuItem("Exit")
        exitItem.addActionListener { 
            onExitClicked?.invoke() 
            System.exit(0)
        }

        popup.add(pauseAllItem)
        popup.add(resumeAllItem)
        popup.addSeparator()
        popup.add(exitItem)

        // Create a simple blank image for the tray icon
        val image = createPlaceholderIcon()

        trayIcon = TrayIcon(image, "KDownloader", popup)
        trayIcon?.isImageAutoSize = true

        try {
            SystemTray.getSystemTray().add(trayIcon)
        } catch (e: AWTException) {
            println("TrayIcon could not be added.")
        }
    }

    fun showNotification(title: String, message: String, isError: Boolean = false) {
        val type = if (isError) TrayIcon.MessageType.ERROR else TrayIcon.MessageType.INFO
        trayIcon?.displayMessage(title, message, type)
    }

    private fun createPlaceholderIcon(): Image {
        val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val graphics = img.createGraphics()
        graphics.color = Color.BLUE
        graphics.fillRect(0, 0, 16, 16)
        graphics.color = Color.WHITE
        graphics.drawString("K", 4, 12)
        graphics.dispose()
        return img
    }
}
