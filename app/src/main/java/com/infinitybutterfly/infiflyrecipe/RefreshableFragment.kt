package com.infinitybutterfly.infiflyrecipe

interface RefreshableFragment {
    // The activity will call this when the user pulls down
    fun onRefreshAction()
}