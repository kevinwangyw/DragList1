package com.fengjian.test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DragListActivity extends Activity {
    
    private static List<String> list = null;
    private DragListAdapter adapter = null;
    private DragListView dragListView;
    
    public static List<String> groupKey= new ArrayList<String>();
    private List<String> navList = new ArrayList<String>();
    private List<String> moreList = new ArrayList<String>();

    private ImageView dragImageView;//被拖拽的项，其实就是一个ImageView
    private int dragSrcPosition;//手指拖动项原始在列表中的位置
    private int dragPosition;//手指拖动的时候，当前拖动项在列表中的位置

    private int dragPoint;//在当前数据项中的位置
    private int dragOffset;//当前视图和屏幕的距离(这里只使用了y方向上)

    private WindowManager windowManager;//windows窗口控制类
    private WindowManager.LayoutParams windowParams;//用于控制拖拽项的显示的参数

    private int scaledTouchSlop;//判断滑动的一个距离
    private int upScrollBounce;//拖动的时候，开始向上滚动的边界
    private int downScrollBounce;//拖动的时候，开始向下滚动的边界

    private GestureDetector gestureDetector;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drag_list_activity);
        
        initData();

        scaledTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        dragListView = (DragListView)findViewById(R.id.drag_list);
        dragListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
        adapter = new DragListAdapter(this, list);
        dragListView.setAdapter(adapter);

        scaledTouchSlop = ViewConfiguration.get(DragListActivity.this).getScaledTouchSlop();
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                System.out.println("onSingleTapUp");
                return super.onSingleTapUp(e);
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                System.out.println("onScroll");
/*                if (dragPosition != dragListView.INVALID_POSITION && dragImageView != null) {
                    int moveY = (int)e2.getY();
                    onDrag(moveY);
                    return true;
                }*/
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                System.out.println("onLongPress(MotionEvent e)");
                int x = (int) e.getX();
                int y = (int) e.getY();

                dragSrcPosition = dragPosition = dragListView.pointToPosition(x, y);  //map a point to a position in the list
                if (dragPosition == AdapterView.INVALID_POSITION) {
                    //return super.onInterceptTouchEvent(ev);
                    return;// super.onDown(e);
                }
                //getChildAt() eturns the view at the specified position in the group.
                //getFirstVisiblePosition() returns the position within the adapter's
                // data set for the first item displayed on screen.
                ViewGroup itemView = (ViewGroup) dragListView.getChildAt(dragPosition - dragListView.getFirstVisiblePosition());
                dragPoint = y - itemView.getTop();  //Top position of this view relative to its parent.
                dragOffset = (int) (e.getRawY() - y);
                // getRawY() returns the original raw Y coordinate of this event.
                //当前视图和屏幕的距离(这里只使用了y方向上)
                View dragger = itemView.findViewById(R.id.drag_list_item_text);
                if (dragger != null && x > dragger.getLeft() && x < dragger.getRight()) {
                    //scaledTouchSlop 判断滑动的一个距离
                    //upScrollBounce 拖动的时候，开始向上滚动的边界
                    upScrollBounce = Math.min(y - scaledTouchSlop, dragListView.getHeight() / 3);
                    downScrollBounce = Math.max(y + scaledTouchSlop, dragListView.getHeight() * 2 / 3);
                    //downScrollBounce 拖动的时候，开始向下滚动的边界
                    itemView.setDrawingCacheEnabled(true);
                    Bitmap bm = Bitmap.createBitmap(itemView.getDrawingCache());
                    startDrag(bm, y);
                    //return true;
                }
            }

            @Override
            public boolean onDown(MotionEvent e) {
                System.out.println("onDown(MotionEvent e)");
                return super.onDown(e);
            }
        });

        dragListView.setOnTouchListener(new MyTouchListener());
    }
    
    public void initData(){
        //数据结果
        list = new ArrayList<String>();
        
        //groupKey存放的是分组标签
        groupKey.add("A组");
        groupKey.add("B组");
        
        for(int i=0; i<5; i++){
            navList.add("A选项"+i);
        }
        list.add("A组");
        list.addAll(navList);
        
        for(int i=0; i<8; i++){
            moreList.add("B选项"+i);
        }
        list.add("B组");
        list.addAll(moreList);
    }
    
    public static class DragListAdapter extends ArrayAdapter<String>{

        public DragListAdapter(Context context, List<String> objects) {
            super(context, 0, objects);
        }
        
        public List<String> getList(){
            return list;
        }
        
        @Override
        public boolean isEnabled(int position) {
            if(groupKey.contains(getItem(position))){
                //如果是分组标签，返回false，不能选中，不能点击
                return false;
            }
            return super.isEnabled(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            
            View view = convertView;
            if(groupKey.contains(getItem(position))){
                //如果是分组标签，就加载分组标签的布局文件，两个布局文件显示效果不同
                view = LayoutInflater.from(getContext()).inflate(R.layout.drag_list_item_tag, null);
            }else{
                //如果是正常数据项标签，就加在正常数据项的布局文件
                view = LayoutInflater.from(getContext()).inflate(R.layout.drag_list_item, null);
            }
            
            TextView textView = (TextView)view.findViewById(R.id.drag_list_item_text);
            textView.setText(getItem(position));
/*            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), "onclick", Toast.LENGTH_SHORT).show();
                    System.out.println("onclick");
                }
            });*/

            return view;
        }
    }

    class MyTouchListener implements View.OnTouchListener {


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(!gestureDetector.onTouchEvent(event)) {
                //Manually handle the event.
                if (event.getAction() == MotionEvent.ACTION_MOVE)
                {
                    //Check if user is actually longpressing, not slow-moving
                    // if current position differs much then press positon then discard whole thing
                    // If position change is minimal then after 0.5s that is a longpress. You can now process your other gestures
                    Log.e("test","Action move");
                    if (dragPosition != dragListView.INVALID_POSITION && dragImageView != null) {
                        int moveY = (int)event.getY();
                        onDrag(moveY);
                        return true;
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    //Get the time and position and check what that was :)
                    Log.e("test","Action down");
                    if (dragPosition != dragListView.INVALID_POSITION && dragImageView != null) {
                        System.out.println("onUP");
                        int upY = (int) event.getY();
                        stopDrag();
                        onDrop(upY);
                    }
                }
            }
            return true;
        }
    }

    /**
     * 准备拖动，初始化拖动项的图像
     *
     * @param bm
     * @param y
     */
    public void startDrag(Bitmap bm, int y) {
        stopDrag();

        windowParams = new WindowManager.LayoutParams();
        windowParams.gravity = Gravity.TOP;
        windowParams.x = 0;
        windowParams.y = y - dragPoint + dragOffset;
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        windowParams.format = PixelFormat.TRANSLUCENT;
        windowParams.windowAnimations = 0;

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bm);
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(imageView, windowParams);
        dragImageView = imageView;
    }

    /**
     * 停止拖动，去除拖动项的头像
     */
    public void stopDrag() {
        if (dragImageView != null) {
            windowManager.removeView(dragImageView);
            dragImageView = null;
        }
    }

    /**
     * 拖动执行，在Move方法中执行
     *
     * @param y
     */
    public void onDrag(int y) {
        if (dragImageView != null) {
            windowParams.alpha = 0.8f;
            windowParams.y = y - dragPoint + dragOffset;
            windowManager.updateViewLayout(dragImageView, windowParams);
        }
        //为了避免滑动到分割线的时候，返回-1的问题
        int tempPosition = dragListView.pointToPosition(0, y);
        if (tempPosition != dragListView.INVALID_POSITION) {
            dragPosition = tempPosition;
        }

        //滚动
        int scrollHeight = 0;
        if (y < upScrollBounce) {
            scrollHeight = 8;//定义向上滚动8个像素，如果可以向上滚动的话
        } else if (y > downScrollBounce) {
            scrollHeight = -8;//定义向下滚动8个像素，，如果可以向上滚动的话
        }

        if (scrollHeight != 0) {
            //真正滚动的方法setSelectionFromTop()
            dragListView.setSelectionFromTop(dragPosition, dragListView.getChildAt(dragPosition - dragListView.getFirstVisiblePosition()).getTop() +
                    scrollHeight);
        }
    }

    /**
     * 拖动放下的时候
     *
     * @param y
     */
    public void onDrop(int y) {

        //为了避免滑动到分割线的时候，返回-1的问题
        int tempPosition = dragListView.pointToPosition(0, y);
        if (tempPosition != dragListView.INVALID_POSITION) {
            dragPosition = tempPosition;
        }

        //超出边界处理
        if (y < dragListView.getChildAt(1).getTop()) {
            //超出上边界
            dragPosition = 1;
        } else if (y > dragListView.getChildAt(dragListView.getChildCount() - 1).getBottom()) {
            //超出下边界
            dragPosition = dragListView.getAdapter().getCount() - 1;
        }

        //数据交换
        if (dragPosition > 0 && dragPosition < dragListView.getAdapter().getCount()) {
            @SuppressWarnings("unchecked")
            DragListAdapter adapter = (DragListAdapter) dragListView.getAdapter();
            String dragItem = adapter.getItem(dragSrcPosition);
            adapter.remove(dragItem);
            adapter.insert(dragItem, dragPosition);
            Toast.makeText(this, adapter.getList().toString(), Toast.LENGTH_SHORT).show();
        }

    }
}