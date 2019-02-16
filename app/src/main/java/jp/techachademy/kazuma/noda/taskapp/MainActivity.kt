package jp.techachademy.kazuma.noda.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmObject
import io.realm.Sort
import io.realm.annotations.PrimaryKey
import kotlinx.android.synthetic.main.activity_main.*
import java.io.Serializable
import java.util.*

//EXTRA_TASK definition as file location
const val EXTRA_TASK = "jp.techachademy.kazuma.noda.taskapp.TASK"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object: RealmChangeListener<Realm> {
        override fun onChange(t: Realm) {
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        fab.setOnClickListener { view ->
            intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)


        mTaskAdapter = TaskAdapter(this)

        listView1.setOnItemClickListener {parent, view, position, id ->
            //retrieve the item task where the list have been tapped
            //set intent to tranfer the page
            //also transfer extra_task
            //start activity (intent)

            val taskHolder = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra("EXTRA_TASK", taskHolder.id)
            startActivity(intent)

        }

        listView1.setOnItemLongClickListener { parent, view, position, id ->
            //get the task that youre trying to delete
            //show the alert diaglog
            //.setTitle , and .setMessage
            //if yes, then delete
                //find the same task object
            //if no then dont delete
            // have to insert true at the end
            //delete add task for test

            val taskHolder = parent.adapter.getItem(position) as Task

            AlertDialog.Builder(this).setTitle("Delete")
                .setPositiveButton("OK") { _, _ ->
                    val result = mRealm.where(Task::class.java).equalTo("id", taskHolder.id).findAll()
                    mRealm.beginTransaction()
                    result.deleteAllFromRealm()
                    mRealm.commitTransaction()

                    reloadListView()

                    val resultIntent = Intent(this, TaskAlarmReceiver::class.java)
                    resultIntent.putExtra("EXTRA_TASK", taskHolder.id)
                    val resultPendingIntent = PendingIntent.getBroadcast(
                        this,
                        taskHolder.id,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT

                    )

                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.cancel(resultPendingIntent)

                }.setNegativeButton("cancel") { _, _ ->

                }.show()

            true
        }
        reloadListView()
    }



    private fun reloadListView() {
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)
        listView1.adapter = mTaskAdapter
        mTaskAdapter.notifyDataSetChanged()
    }



    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }



    private fun addTaskForTest() {
        val task = Task()
        task.title = "test"
        task.contents = "testContents"
        task.date = Date()
        task.id = 0
        mRealm.beginTransaction()
        mRealm.copyToRealmOrUpdate(task)
        mRealm.commitTransaction()
    }
}



open class Task: RealmObject(), Serializable {
    var title: String = ""
    var contents: String = ""
    var date: Date = Date()

    @PrimaryKey
    var id: Int = 0
}
