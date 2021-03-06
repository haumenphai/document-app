package dotd.hmp.fragment

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import dotd.hmp.R
import dotd.hmp.activities.ViewDetailRecordActivity
import dotd.hmp.activities.ViewRecordsActivity
import dotd.hmp.adapter.GroupsRecordExpandAdapater
import dotd.hmp.adapter.RecordAdapater
import dotd.hmp.data.*
import dotd.hmp.databinding.FragmentViewRecordsBinding
import dotd.hmp.dialog.*
import dotd.hmp.hepler.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.*
import kotlin.collections.HashSet
import kotlin.system.measureTimeMillis


@SuppressLint("SetTextI18n")

class ViewRecordsFragment : Fragment() {
    private lateinit var b: FragmentViewRecordsBinding
    private val act: ViewRecordsActivity by lazy { activity as ViewRecordsActivity }
    private val adapter: RecordAdapater = RecordAdapater()
    private val groupRecordAdapater: GroupsRecordExpandAdapater by lazy {
        GroupsRecordExpandAdapater(act).apply {
            b.expandedListView.setAdapter(this)
        }
    }
    private lateinit var model: Model

    private var filterRecord: FilterRecord? = null
    private var isFiltering: Boolean = false
    private var fieldForGroup: Field? = null
    private var isGrouping: Boolean = false

    private var isSorting: Boolean = false
    private var fieldForSort: Field? = null
    private var reverseSort: Boolean = false

    private var viewMode = ViewMODE.LIST


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        b = FragmentViewRecordsBinding.inflate(inflater, container, false)
        hideViewByDefault()
        setConfigToolBar()
        setUpRecyclerView()
        setUpLayoutAction()
        setUpSearchView()
        setUpSortRecords()
        setUpViewMode()

        return b.root
    }

    override fun onStart() {
        super.onStart()
        TimeDelay.setTime(200).onFinish { webView.visibility = View.INVISIBLE }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_view_data_model_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> act.addFragment(AddRecordFragment(), "add_record_fragment")
            R.id.menu_search -> showSearchView()
            R.id.menu_analysis -> showDialogAnalysis()
        }
        return super.onOptionsItemSelected(item)
    }

    private val layoutWebView: FrameLayout by lazy {
        layoutInflater.inflate(R.layout.view_webview, b.root, false) as FrameLayout
    }
    private val webView: WebView by lazy {
        val webView: WebView = layoutWebView.findViewById(R.id.web_view)
        webView.settings.apply {
            displayZoomControls = false
            useWideViewPort = true
            javaScriptEnabled = true
            builtInZoomControls = true
        }


        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d("AAA", consoleMessage.message() + " -- From line "
                            + consoleMessage.lineNumber() + " of "
                            + consoleMessage.sourceId()
                )
                return super.onConsoleMessage(consoleMessage)
            }
        }
        webView.addJavascriptInterface(this, "app")
        layoutWebView.removeAllViews()
        b.container.addView(webView)
        webView
    }

    private fun setUpViewMode() {
        b.imgList.setBackgroundColor(Color.parseColor("#B6B6B6"))

        b.imgTable.setOnClickListener {
            webView.visibility = View.VISIBLE
            this.viewMode = ViewMODE.TABLE
            b.imgTable.setBackgroundColor(Color.parseColor("#B6B6B6"))
            b.imgList.setBackgroundResource(R.drawable.rippler_blue_white)
            pagingForRecords(model.getRecordList())
        }
        b.imgList.setOnClickListener {
            webView.visibility = View.INVISIBLE
            this.viewMode = ViewMODE.LIST
            b.imgList.setBackgroundColor(Color.parseColor("#B6B6B6"))
            b.imgTable.setBackgroundResource(R.drawable.rippler_blue_white)
            pagingForRecords(model.getRecordList())
        }
    }

    private fun loadDataViewTable(records: List<JsonObject>) {
        if (viewMode == ViewMODE.TABLE) {
            webView.loadDataWithBaseURL(
                null, model.toHtmlTable(records),
                "text/html; charset=utf-8", "UTF-8", null
            )
        }
    }



    private fun setUpRecyclerView() {
        act.model.observeForever {
            this.model = it
            pagingForRecords(model.getRecordList())
            b.tvNoRecord.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }

        b.recyclerView.layoutManager = LinearLayoutManager(act)
        b.recyclerView.adapter = adapter
        setClickRecord()
    }

    private fun pagingForRecords(_records: List<JsonObject>) {
        var records = _records

        val maxRecordsShowed = 100
        var start = 0
        var end = maxRecordsShowed
        var currentRecords: List<JsonObject> = listOf()

        if (isSorting) {
            records = sortRecords(records)
        }

        fun initValue(records: List<JsonObject>) {
            b.tvRecordsCount.text = records.size.toString()
            if (records.size < maxRecordsShowed) {
                end = records.size
            }
            currentRecords = records.subList(start, end)
            adapter.setRecordList(currentRecords)
            adapter.setStartIndex(start)
            b.tvRecordsCurrent.text = "${start+1}-$end / "
        }


        initValue(records)
        if (isFiltering) {
            filterRecord(records) {
                records = it
                if (isSorting) {
                    records = sortRecords(it)
                }
                initValue(records)
                loadDataViewTable(currentRecords)
            }
        }
        loadDataViewTable(currentRecords)


        b.imgRight.setOnClickListener {
            start += maxRecordsShowed
            end += maxRecordsShowed
            if (start >= records.size) {
                start = 0
                end = maxRecordsShowed
            }
            if (end >= records.size) {
                end = records.size
            }

            currentRecords = records.subList(start, end)
            adapter.setRecordList(currentRecords)
            adapter.setStartIndex(start)
            b.tvRecordsCurrent.text = "${start+1}-$end / "
            b.tvRecordsCount.text = records.size.toString()

            if (isGrouping) {
                groupRecord(currentRecords)
            }
            loadDataViewTable(currentRecords)
        }
        b.imgLeft.setOnClickListener {
            start -= maxRecordsShowed
            end -= maxRecordsShowed
            if (start < 0) {
                start = records.size - maxRecordsShowed
                end = records.size
                if (start < 0) {
                    start = 0
                }
            }

            currentRecords = records.subList(start, end)
            adapter.setRecordList(currentRecords)
            adapter.setStartIndex(start)
            b.tvRecordsCurrent.text = "${start+1}-$end / "
            b.tvRecordsCount.text = records.size.toString()

            if (isGrouping) {
                groupRecord(currentRecords)
            }
            loadDataViewTable(currentRecords)
        }
        setUpFilterGroup(currentRecords)
    }

    private val dialogSortRecord by lazy { DialogSortRecords(act, model.getFieldList().toMutableList()) }
    private fun setUpSortRecords() {
        dialogSortRecord.setOnClickItem { field, reverse ->
            this.fieldForSort = field
            this.reverseSort = reverse
            this.isSorting = true
            pagingForRecords(model.getRecordList())
        }
        b.imgSort.setOnClickListener {
            dialogSortRecord.show()
        }
    }

    private fun sortRecords(records: List<JsonObject>): MutableList<JsonObject> {
        val list = model.sortByField(fieldForSort!!, records).getRecordList()
        if (reverseSort)
            list.reverse()
        return list
    }


    private val dialogGroupsRecord by lazy { DialogPickFieldForGroup(act, model.getFieldList()) }
    private val dialogFilterRecord by lazy { DialogFilterRecord(act, model) }

    private fun setUpFilterGroup(recordsPaging: List<JsonObject>) {
        dialogGroupsRecord.setOnclickItem { field ->
            this.isGrouping = true
            this.fieldForGroup = field
            this.isFiltering = false
            this.filterRecord = null
            groupRecord(recordsPaging)
            pagingForRecords(model.getRecordList())
            dialogFilterRecord.unCheckAll()
        }

        dialogFilterRecord.setClickItem { filterRecord ->
            this.isFiltering = true
            this.filterRecord = filterRecord
            this.isGrouping = false
            this.fieldForGroup = null
            pagingForRecords(model.getRecordList())
            closeLayoutGroup()
            dialogGroupsRecord.unCheckAll()
        }

        b.imgGroup.setOnClickListener {
            dialogGroupsRecord.show()
        }
        b.imgFilter.setOnClickListener {
            dialogFilterRecord.show()
        }

        b.imgCloseFilterGroup.setOnClickListener {
            closeLayoutGroup()
            closeLayoutFilter()
            dialogGroupsRecord.unCheckAll()
            dialogFilterRecord.unCheckAll()

            this.isFiltering = false
            this.isGrouping = false
            this.filterRecord = null
            this.fieldForGroup = null
            pagingForRecords(model.getRecordList())
        }
    }

    private fun filterRecord(records: List<JsonObject>, onCompleted: (records: List<JsonObject>) -> Unit) {
        GlobalScope.launch {
            val loading = DialogLoadingFullScreen(act, b.root)
            loading.show()

            val jobList = mutableListOf<Job>()
            val motherOfRecords: List<List<JsonObject>> = if (records.size >= 2) {
                records.chunked(records.size / 2)
            } else {
                records.chunked(records.size)
            }
            val resultSet = HashSet<JsonObject>()

            motherOfRecords.forEach {
                val job = launch {
                    val model = model.filter(filterRecord!!, records)
                    resultSet.addAll(model.getRecordList())
                }
                jobList.add(job)
            }
            jobList.forEach { it.join() }


            withContext(Dispatchers.Main) {
                loading.cancel()
                showLayoutFiler()
                b.tvFilterGroup.text = "${getStr(R.string.filter_by)}: $filterRecord"
                onCompleted(resultSet.toList())
            }
        }

    }

    private fun groupRecord(recordsPaging: List<JsonObject>) {
        val loading = DialogLoadingFullScreen(act, b.root)
        loading.show()

        GlobalScope.launch {
            val diffValues: List<String> = model.getDifferentFieldValue(fieldForGroup!!, recordsPaging)
            val hashMapChild = HashMap<String, List<JsonObject>>()

            val jobList = mutableListOf<Job>()
            diffValues.forEach {
                val job = launch {
                    val list = model.getRecordsHasValueOfField(fieldForGroup!!, it, recordsPaging)
                    hashMapChild.put(it, list)
                }
                jobList.add(job)
            }
            jobList.forEach { it.join() }

            withContext(Dispatchers.Main) {
                groupRecordAdapater.setData(act, diffValues, hashMapChild, fieldForGroup!!)
                showLayoutGroup()
                b.tvFilterGroup.text = "${getStr(R.string.group_by)}: ${fieldForGroup!!.fieldName}"
                loading.cancel()
            }
        }
    }

    private fun closeLayoutGroup() {
        b.expandedListView.visibility = View.GONE
        b.layoutInfoFilterGroup.visibility = View.GONE
        b.recyclerView.visibility = View.VISIBLE
        cancelAction()
    }

    private fun showLayoutGroup() {
        b.expandedListView.visibility = View.VISIBLE
        b.layoutInfoFilterGroup.visibility = View.VISIBLE
        b.recyclerView.visibility = View.GONE
        cancelAction()
    }

    private fun closeLayoutFilter() {
        b.layoutInfoFilterGroup.visibility = View.GONE
        b.tvRecordsCount.text = model.getRecordList().size.toString()
    }

    private fun showLayoutFiler() {
        b.layoutInfoFilterGroup.visibility = View.VISIBLE
    }

    private fun setClickRecord() {
        fun onClickItem(record: JsonObject) {
            val intent = Intent(context, ViewDetailRecordActivity::class.java)
            intent.putExtra("model", act.model.value)
            intent.putExtra("recordString", record.toString())
            startActivityForResult(intent, 123)
        }

        fun onLongClickItem(record: JsonObject) {
            b.layoutActionRecords.root.visibility = View.VISIBLE
            record.addProperty("is_selected", true)
            hideActionEdit()

            fun onClickItemOverride(record: JsonObject) {
                try {
                    val isSelected = record.get("is_selected").asBoolean
                    record.addProperty("is_selected", !isSelected)
                } catch (e: Exception) {
                    record.addProperty("is_selected", true)
                }
                adapter.notifyDataSetChanged()
                groupRecordAdapater.notifyDataSetChanged()
                hideActionEdit()
            }

            adapter.onClickItem = { onClickItemOverride(it) }
            groupRecordAdapater.onClickItem = { onClickItemOverride(it) }

            adapter.notifyDataSetChanged()
            groupRecordAdapater.notifyDataSetChanged()
        }

        adapter.onClickItem = { onClickItem(it) }
        adapter.onLongClickItem = { onLongClickItem(it) }
        groupRecordAdapater.onClickItem = { onClickItem(it) }
        groupRecordAdapater.onLongClickItem = { onLongClickItem(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            val model = data.getSerializableExtra("model") as Model
            act.model.value = model
        }
    }

    private fun setConfigToolBar() {
        act.setSupportActionBar(b.toolBar)
        b.appbarLayout.outlineProvider = null
        act.supportActionBar!!.title = act.model.value!!.name
    }

    private fun cancelAction() {
        adapter.unSelectAll()
        groupRecordAdapater.unSelectAll()
        b.layoutActionRecords.root.visibility = View.GONE
        setClickRecord()
    }

    private fun setUpLayoutAction() {
        val binding = b.layoutActionRecords

        binding.actionSelectAll.setOnClickListener {
            adapter.selectAll()
            groupRecordAdapater.selectAll()
            hideActionEdit()
        }
        binding.actionDelete.setOnClickListener {
            val set = HashSet<JsonObject>()
            set.addAll(adapter.getRecordsSeleted())
            set.addAll(groupRecordAdapater.getRecordsSelected())

            val recordsSelected = set.toMutableList()
            if (recordsSelected.isEmpty()) {
               return@setOnClickListener
            }

            DialogConfirm(act)
                .setTitle(getStr(R.string.delete))
                .setMess("${getStr(R.string.delete)} ${recordsSelected.size} ${getString(R.string.record)}?")
                .setTextBtnOk(getStr(R.string.delete))
                .setTextBtnCancel(getStr(R.string.Cancel))
                .setBtnOkClick {
                    recordsSelected.forEach {
                        model.deleteRecord(it)
                        act.model.value = model
                        ModelDB.update(model = model)
                        cancelAction()
                    }
                    it.cancel()
                }
                .setBtnCancelClick {
                    it.cancel()
                }.show()
        }
        binding.actionEdit.setOnClickListener {
            // only one record can be edit
            val intent = Intent(context, ViewDetailRecordActivity::class.java)

            //
            val record = try {
                adapter.getRecordsSeleted()[0].asJsonObject
            } catch (e: java.lang.Exception) {
                groupRecordAdapater.getRecordsSelected()[0].asJsonObject
            }

            record.remove("is_selected")
            cancelAction()

            intent.putExtra("recordString", record.toString())
            intent.putExtra("model", model)
            intent.action = ViewDetailRecordActivity.ACTION_EDIT_RECORD

            startActivityForResult(intent, 123)
        }
        binding.actionCancel.setOnClickListener {
           cancelAction()
        }
    }

    private fun setUpSearchView() {
        var withField: MutableList<Field>? = null
        b.editSearch.addTextChangedListener {
           searchRecords(it.toString(), withField = withField)
        }
        b.editSearch.setOnEditorActionListener { v, actionId, event ->
            searchRecords(b.editSearch.text.toString(), "exact", withField)
            UIHelper.hideKeyboardFrom(act, b.root)
            true
        }
        b.imgSearchClose.setOnClickListener {
            closeSearchView()
        }

        val fieldNameList = model.getFieldList().map { it.fieldName.toFieldNameShow() }.toMutableList()
        fieldNameList.add(getString(R.string.default_field_create_time).toFieldNameShow())
        fieldNameList.add(0,getString(R.string.all))
        b.spinnerSearchByField.adapter =
            ArrayAdapter(act, R.layout.support_simple_spinner_dropdown_item, fieldNameList)
        b.spinnerSearchByField.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                withField = null
                val fieldName = fieldNameList[position].toFieldNameStore()
                if (fieldName != getStr(R.string.all)) {
                    withField = mutableListOf(model.getField(fieldName)!!)
                }
                searchRecords(b.editSearch.text.toString(), withField = null)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun hideViewByDefault() {
        b.layoutSearch.visibility = View.GONE
        b.layoutActionRecords.root.visibility = View.GONE
        b.iconLoadingSearch.visibility = View.GONE
        b.layoutInfoFilterGroup.visibility = View.GONE
    }

    private fun hideActionEdit() {
        if (adapter.getRecordsSeleted().size != 1 && groupRecordAdapater.getRecordsSelected().size != 1) {
            b.layoutActionRecords.actionEdit.visibility = View.INVISIBLE
        } else {
            b.layoutActionRecords.actionEdit.visibility = View.VISIBLE
        }
    }

    private fun closeSearchView() {
        b.layoutSearch.visibility = View.GONE
        b.iconLoadingSearch.visibility = View.GONE
        b.editSearch.setText("")
        UIHelper.hideKeyboardFrom(act, b.root)
        adapter.setModel(model)
    }

    private fun showSearchView() {
        UIHelper.showKeyboard(act)
        b.editSearch.requestFocus()
        b.layoutSearch.visibility = View.VISIBLE
    }

    private var job1: Job? = null
    private fun searchRecords(key: String, searchMethod: String = "relative", withField: MutableList<Field>? = null)  {
        job1?.cancel()
        b.iconLoadingSearch.visibility = View.VISIBLE
        val maxThread = Runtime.getRuntime().availableProcessors()

        job1 = GlobalScope.launch {
            val originalList = model.getRecordList()
            if (originalList.isEmpty()) {
                withContext(Dispatchers.Main) {
                    b.iconLoadingSearch.visibility = View.GONE
                }
                return@launch
            }

            val motherOfList = if (originalList.size >= maxThread) {
                originalList.chunked(originalList.size / maxThread).toMutableList()
            }  else {
                originalList.chunked(originalList.size).toMutableList()
            }
            val resultSet = HashSet<JsonObject>()

            val time = measureTimeMillis {
                val jobList = mutableListOf<Job>()
                motherOfList.forEach {
                    val job = launch {
                        val model = model.searchRecords(key, searchMethod = searchMethod, records = it, _withField = withField)
                        resultSet.addAll(model.getRecordList())
                    }
                    jobList.add(job)
                }
                jobList.forEach { it.join() }
            }
            Log.d("AAA", time.toString())
            Log.d("AAA", Runtime.getRuntime().availableProcessors().toString())

            withContext(Dispatchers.Main) {
                val resultSearch = resultSet.toMutableList()
                pagingForRecords(resultSearch)
                b.tvNoRecord.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                b.iconLoadingSearch.visibility = View.GONE
            }
        }
    }

    @JavascriptInterface
    fun viewRecord(recordID: String) {
        val intent = Intent(context, ViewDetailRecordActivity::class.java)
        intent.putExtra("model", act.model.value)
        intent.putExtra("recordString", model.getRecord(recordID).toString())
        startActivityForResult(intent, 123)
    }

    private fun showDialogAnalysis() {
        DialogShowTextLarge(act)
            .setTitle(getStr(R.string.analysis_title_dialog))
            .setContentHTML(getContentAnalysic())
            .show()
    }

    private fun getContentAnalysic(): String {
        var result = ""
        val records = model.getRecordList()
        val maxTitle = getStr(R.string.max)
        val minTitle = getStr(R.string.min)
        val avgTitle = getStr(R.string.avg)
        val sumTitle = getStr(R.string.sum)

        fun sum(list: List<Double>): Double {
            var _result = 0.0
            list.forEach { _result += it }
            return _result
        }

        fun avg(list: List<Double>): Double {
            return sum(list) / list.size
        }

        fun max(list: List<Double>): Double {
            var max = Double.MIN_VALUE
            list.forEach {
                if (it > max) max = it
            }
            return  max
        }

        fun min(list: List<Double>): Double {
            var min = Double.MAX_VALUE
            list.forEach {
                if (it < min) min = it
            }
            return  min
        }


        for (field in model.getFieldList()) {
            if (field.fieldType == FieldType.NUMBER) {
                val valueList = mutableListOf<Double>()
                for (record in records) {
                    valueList.add(record.getValueOfField(field).toDouble())
                }

                result += """
                        <font color="black">
                            <b>${field.fieldName}:</b>
                        </font>
                        <br>
                        $maxTitle: ${formatDouble(max(valueList), 2)} <br>
                        $minTitle: ${formatDouble(min(valueList), 2)} <br>
                        $sumTitle: ${formatDouble(sum(valueList), 2)} <br>
                        $avgTitle: ${formatDouble(avg(valueList), 2)} <br>
                        _____________________________
                        <br>
                    """.trimIndent()
            } else {
                // todo: check field type: String
                continue
            }
        }

        return result
    }


    private enum class ViewMODE {
        LIST, TABLE
    }
}
