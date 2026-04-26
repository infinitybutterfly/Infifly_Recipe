package com.infinitybutterfly.infiflyrecipe

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    var randomLetterPopular: String = "a"
    var randomLetterForYou: String = "z"

    lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        randomLetterPopular = ('a'..'z').random().toString()
        randomLetterForYou = ('a'..'z').random().toString()

        swipeRefreshLayout = findViewById(R.id.mainSwipeRefreshLayout)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()

        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.auth_nav_graph)

        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val jwtToken = sharedPref.getString("JWT_TOKEN", null)
        val isProfileComplete = sharedPref.getBoolean("isProfileComplete", false)
//        val isLocked = sharedPref.getBoolean("isLocked", false)

        if (!jwtToken.isNullOrEmpty() && (!isProfileComplete or isProfileComplete)) {
            graph.setStartDestination(R.id.main_nav_graph)
            Log.d("JWT_Token","JWT Token found")
        } else {
            graph.setStartDestination(R.id.loginWelcomeFragment)
            Log.d("JWT_Token","JWT Token not found")

        }

        navController.graph = graph

//        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fab = findViewById<FloatingActionButton>(R.id.add_button_nav)

        bottomNavigationView.setupWithNavController(navController)

        fab.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build()
            navController.navigate(R.id.addRecipeFragment, null, navOptions)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.discoverRecipeFragment,
//                R.id.addRecipeFragment,
                R.id.myRecipeFragment,
                R.id.profileFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    fab.show()

                    bottomNavigationView.translationY = 0f
                    fab.translationY = 0f
                }
                else -> {
                    bottomNavigationView.visibility = View.GONE
                    fab.hide()
                }
            }
        }

        swipeRefreshLayout.setOnRefreshListener {

            // 1. Grab the top level NavHost
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as? NavHostFragment

            // 2. Use our new helper function to dig through the nested graphs!
            val currentFragment = getVisibleFragment(navHostFragment)

            if (currentFragment is RefreshableFragment) {
                android.widget.Toast.makeText(this, "Refreshing data...", android.widget.Toast.LENGTH_SHORT).show()

                currentFragment.onRefreshAction()
            } else {
                stopRefreshAnimation()
            }
        }
    }

    private fun getVisibleFragment(fragment: androidx.fragment.app.Fragment?): androidx.fragment.app.Fragment? {
        if (fragment is NavHostFragment) {
            return getVisibleFragment(fragment.childFragmentManager.primaryNavigationFragment)
        }
        return fragment
    }

    fun setSwipeRefreshEnabled(isEnabled: Boolean) {
        swipeRefreshLayout.post {
            swipeRefreshLayout.isEnabled = isEnabled
        }
    }

    // Using .post {} guarantees it kills the spinner safely on the UI thread
    fun stopRefreshAnimation() {
        swipeRefreshLayout.post {
            swipeRefreshLayout.isRefreshing = false
        }
    }
}

//        swipeRefreshLayout.setOnRefreshListener {
//
//            val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
//
//            if (currentFragment is RefreshableFragment) {
//                currentFragment.onRefreshAction()
//            } else {
//                swipeRefreshLayout.isRefreshing = false
//            }
//        }
//    }
//    fun setSwipeRefreshEnabled(isEnabled: Boolean) {
//        swipeRefreshLayout.isEnabled = isEnabled
//    }
//
//    fun stopRefreshAnimation() {
//        swipeRefreshLayout.isRefreshing = false
//    }
//}



//package com.infinitybutterfly.infiflyrecipe
//
//import android.os.Bundle
//import android.view.View
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import androidx.navigation.fragment.NavHostFragment
//import androidx.navigation.ui.setupWithNavController
//import com.google.android.material.bottomappbar.BottomAppBar
//
//class MainActivity : AppCompatActivity() {
//    var randomLetterPopular: String = "a"
//    var randomLetterForYou: String = "z"
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//        randomLetterPopular = ('a'..'z').random().toString()
//        randomLetterForYou = ('a'..'z').random().toString()
//
//        val navHostFragment = supportFragmentManager
//            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
//        val navController = navHostFragment.navController
//
//        BottomAppBar.setupWithNavController(navController)
//
//        navController.addOnDestinationChangedListener { _, destination, _ ->
//            when (destination.id) {
//                R.id.home_button_nav,
//                R.id.discover_button_nav,
//                R.id.add_button_nav,
//                R.id.my_recipe_button_nav,
//                R.id.profile_button_nav -> {
//                    BottomAppBar.visibility = View.VISIBLE
//                }
//                else -> {
//                    BottomAppBar.visibility = View.GONE
//                }
//            }
//        }
//    }
//}