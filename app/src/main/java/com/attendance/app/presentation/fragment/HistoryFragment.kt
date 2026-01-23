package com.attendance.app.presentation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.attendance.app.databinding.FragmentHistoryBinding
import com.attendance.app.presentation.adapter.AttendanceAdapter
import com.attendance.app.presentation.viewmodel.HistoryViewModel
import com.attendance.app.utils.CsvExporter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: AttendanceAdapter

    @Inject
    lateinit var csvExporter: CsvExporter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = AttendanceAdapter()
        binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendance.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allAttendance.observe(viewLifecycleOwner) { attendanceList ->
            adapter.submitList(attendanceList)

            if (attendanceList.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvAttendance.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvAttendance.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnExportCsv.setOnClickListener {
            exportToCsv()
        }
    }

    private fun exportToCsv() {
        val attendanceList = viewModel.allAttendance.value

        if (attendanceList.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Không có dữ liệu để xuất", Toast.LENGTH_SHORT).show()
            return
        }

        val file = csvExporter.exportToCsv(attendanceList)

        if (file != null) {
            val shareIntent = csvExporter.shareCsvFile(file)
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ file CSV"))
            Toast.makeText(requireContext(), "✓ Đã xuất file CSV", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Lỗi xuất file CSV", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}