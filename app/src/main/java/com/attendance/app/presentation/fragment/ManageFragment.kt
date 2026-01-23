package com.attendance.app.presentation.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.attendance.app.databinding.FragmentManageBinding
import com.attendance.app.presentation.adapter.StudentAdapter
import com.attendance.app.presentation.viewmodel.StudentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManageFragment : Fragment() {

    private var _binding: FragmentManageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentViewModel by viewModels()
    private lateinit var adapter: StudentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = StudentAdapter(
            onDeleteClick = { student ->
                showDeleteConfirmation(student.name) {
                    viewModel.deleteStudent(student)
                }
            }
        )

        binding.rvStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudents.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allStudents.observe(viewLifecycleOwner) { students ->
            adapter.submitList(students)

            if (students.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvStudents.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvStudents.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmation()
        }
    }

    private fun showDeleteConfirmation(studentName: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa học sinh")
            .setMessage("Bạn có chắc muốn xóa $studentName?")
            .setPositiveButton("Có") { _, _ -> onConfirm() }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Cảnh báo")
            .setMessage(
                "Bạn có chắc muốn xóa TOÀN BỘ dữ liệu?\n\n" +
                        "Điều này sẽ xóa:\n" +
                        "- Tất cả học sinh\n" +
                        "- Tất cả dữ liệu nhận diện\n\n" +
                        "Hành động này KHÔNG THỂ hoàn tác!"
            )
            .setPositiveButton("Có") { _, _ ->
                viewModel.deleteAllStudents()
            }
            .setNegativeButton("Không", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}