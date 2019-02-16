package jp.techachademy.kazuma.noda.taskapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import io.realm.Realm
import kotlinx.android.synthetic.main.content_input.*
import java.time.Year
import java.util.*

class InputActivity : AppCompatActivity() {

    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0
    private var mTask: Task? = null

    //
    private val mOnDateClickListener = View.OnClickListener {
        DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                mYear = year
                mMonth = month
                mDay = dayOfMonth
                date_button.text = mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)

            },mYear, mMonth, mDay).show()
    }

    private val mOnTimerClickLister = View.OnClickListener {
        TimePickerDialog(this,
            TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                mHour = hourOfDay
                mMinute = minute
                times_button.text = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)

            }, mHour, mMinute, false).show()

    }

    private val onDoneClickListener = View.OnClickListener {
        addTask()
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        //setting actionbar, finding from R resource, and setsupportactionbar method enable it
        //also setdisplayhomeasupenable will display the returning button
        setSupportActionBar(findViewById(R.id.toolbar))
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }


        //inserting the onclick to the buttons
        date_button.setOnClickListener(mOnDateClickListener)
        times_button.setOnClickListener(mOnTimerClickLister)
        done_button.setOnClickListener(onDoneClickListener)



        //retrieve task object by searching the task id
        //initialize intent > retreive id from intent > initialize realm
        // > search the task object from realm based on the id > close realm
        val intent = intent
        val taskId = intent.getIntExtra("EXTRA_TASK", -1)
        val realm = Realm.getDefaultInstance()
        mTask = realm.where(Task::class.java).equalTo("id", taskId).findFirst()
        realm.close()


        if (mTask == null) {
            // retreive the present time by chalender class, if the task object is null(creating the first task)
            val calendar = Calendar.getInstance()
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR)
            mMinute = calendar.get(Calendar.MINUTE)


        } else {
            //editing the task
            //retrieving the information based on the previous set task
            //showing that to the button
            title_edit_text.setText(mTask!!.title)
            content_edit_text.setText(mTask!!.contents)

            val calendar = Calendar.getInstance()
            calendar.time = mTask!!.date
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR)
            mMinute = calendar.get(Calendar.MINUTE)

            date_button.text = mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)
            times_button.text = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)

        }




    }


    private fun addTask() {
        //moving the values to task fields
        //first identifying the id
        //the rest of the variables are just copy and pasting

        val realm = Realm.getDefaultInstance()

        realm.beginTransaction()

        if (mTask == null) {
            mTask = Task()
            val result = realm.where(Task::class.java).findAll()


            if (result.max("id") != null) {
                mTask!!.id= result.max("id")!!.toInt() + 1
            } else {
                mTask!!.id = 0
            }
        }

        mTask!!.title = title_edit_text.text.toString()
        mTask!!.contents = content_edit_text.text.toString()

        val calendar = GregorianCalendar(mYear,mMonth,mMonth,mDay,mHour,mMinute)
        mTask!!.date = calendar.time

        realm.copyToRealmOrUpdate(mTask)
        realm.commitTransaction()
        realm.close()

        val resultIntent = Intent(this, TaskAlarmReceiver::class.java)
        resultIntent.putExtra("EXTRA_TASK", mTask!!.id)
        val resultPendingIntent = PendingIntent.getBroadcast(
            this,
            mTask!!.id,
            resultIntent,
            FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, resultPendingIntent)



    }
}
