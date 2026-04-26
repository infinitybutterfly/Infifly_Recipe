package com.infinitybutterfly.infiflyrecipe

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.infinitybutterfly.infiflyrecipe.adapters.RvAdapterDiscoveryRecipe
import com.infinitybutterfly.infiflyrecipe.databinding.FragmentDiscoverRecipeBinding
import com.infinitybutterfly.infiflyrecipe.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiscoverRecipeFragment : Fragment(), RefreshableFragment {

    override fun onRefreshAction() {
                fetchDiscoveryRecipes()
    }
    private var _binding: FragmentDiscoverRecipeBinding? = null
    private val binding get() = _binding!!
    private var rvAdapterDiscovery = RvAdapterDiscoveryRecipe(emptyList()) { clickedRecipe ->
        Log.d("CLICKED", "User clicked on Discovery recipe: ${clickedRecipe.strCategory}")
        Toast.makeText(requireContext(), "User clicked on Discovery recipe: ${clickedRecipe.strCategory}", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

//        val randomQuery = ('a'..'z').random().toString()
//        fetchDiscoveryRecipes(randomQuery)
        fetchDiscoveryRecipes()
    }

    private fun setupRecyclerView() {
        binding.recyclerviewDiscover.apply {
//            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = rvAdapterDiscovery
        }
    }

    private fun fetchDiscoveryRecipes() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.discoveryApi.getDiscoveryRecipe()

                if (response.isSuccessful) {
                    val categoriesList = response.body()?.categories ?: emptyList()
                    val filteredList = categoriesList.drop(1)
                    Log.d("API_SUCCESS", "Successfully fetched ${categoriesList.size} discovery categories!")

                    withContext(Dispatchers.Main) {
                        rvAdapterDiscovery.updateData(filteredList)
                    }
                } else {
                    Log.e("API_ERROR", "Discovery Server error code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API_CRASH", "Discovery Network request failed: ${e.message}")
            } finally {
                mainActivity?.stopRefreshAnimation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




//package com.infinitybutterfly.infiflyrecipe
//
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//
//// TODO: Rename parameter arguments, choose names that match
//// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"
//
///**
// * A simple [Fragment] subclass.
// * Use the [DiscoverRecipeFragment.newInstance] factory method to
// * create an instance of this fragment.
// */
//class DiscoverRecipeFragment : Fragment() {
//    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_discover_recipe, container, false)
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment DiscoverRecipeFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            DiscoverRecipeFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}