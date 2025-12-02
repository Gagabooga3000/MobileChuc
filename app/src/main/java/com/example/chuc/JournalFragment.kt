package com.example.chuc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.presentation.viewmodel.JournalSubjectUiModel
import com.example.chuc.presentation.viewmodel.ProfileViewModel
import com.example.chuc.schedule.UiState

class JournalFragment : Fragment() {

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var courseSpinner: Spinner
    private lateinit var subjectSpinner: Spinner
    private lateinit var subjectLabel: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var backButton: View
    private lateinit var adapter: JournalEntriesAdapter

    private var currentCourse = 1
    private var subjects: List<JournalSubjectUiModel> = emptyList()
    private var suppressSubjectListener = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        bindViews(view)
        setupCourseSpinner()
        setupSubjectSpinner()
        setupRecycler()
        observeViewModel()

        profileViewModel.loadCourseSubjects(currentCourse)
    }

    private fun bindViews(view: View) {
        courseSpinner = view.findViewById(R.id.course_spinner)
        subjectSpinner = view.findViewById(R.id.journal_subject_spinner)
        subjectLabel = view.findViewById(R.id.journal_subject_label)
        recyclerView = view.findViewById(R.id.journal_recycler)
        progressBar = view.findViewById(R.id.journal_progress)
        errorText = view.findViewById(R.id.journal_error)
        backButton = view.findViewById(R.id.journal_back_button)
        backButton.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupCourseSpinner() {
        val courses = listOf("1 курс", "2 курс", "3 курс", "4 курс")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, courses)
        courseSpinner.adapter = adapter
        courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newCourse = position + 1
                if (currentCourse != newCourse) {
                    currentCourse = newCourse
                }
                subjectSpinner.visibility = View.GONE
                subjectLabel.visibility = View.GONE
                profileViewModel.loadCourseSubjects(currentCourse)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSubjectSpinner() {
        subjectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSubjectListener) return
                if (position in subjects.indices) {
                    profileViewModel.loadCourseJournal(currentCourse, subjects[position].id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecycler() {
        adapter = JournalEntriesAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        profileViewModel.courseSubjectsState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    subjectSpinner.visibility = View.GONE
                    subjectLabel.visibility = View.GONE
                }
                is UiState.Success -> {
                    subjects = state.data
                    suppressSubjectListener = true
                    val titles = subjects.map { it.name }
                    val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, titles)
                    subjectSpinner.adapter = spinnerAdapter
                    subjectLabel.visibility = View.VISIBLE
                    subjectSpinner.visibility = View.VISIBLE
                    subjectSpinner.setSelection(0, false)
                    suppressSubjectListener = false
                    val subjectId = subjects.firstOrNull()?.id
                    profileViewModel.loadCourseJournal(currentCourse, subjectId)
                }
                is UiState.Error -> {
                    subjectSpinner.visibility = View.GONE
                    subjectLabel.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.message
                }
            }
        })

        profileViewModel.courseJournalState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                }
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    adapter.submitList(state.data)
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.message
                }
            }
        })
    }

    companion object {
        fun newInstance(): JournalFragment = JournalFragment()
    }
}

