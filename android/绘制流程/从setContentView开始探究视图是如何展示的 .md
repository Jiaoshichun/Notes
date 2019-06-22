## setContentView都做了些什么事  
Activity代码很简单,调用getWindow().setContentView(layoutResID),即调用了PhoneWindow的setContent()方法   
贴一下PhoneWindow的setContent方法  
```
@Override
public void setContentView(int layoutResID) {
   //安装DecorView
    if (mContentParent == null) {
        installDecor();
    } 
    // ·······省略部分代码·······
        //将设置的layoutResID的布局放入到 mContentParent中  
        mLayoutInflater.inflate(layoutResID, mContentParent);
    // ·······省略部分代码·······
}
```  
关键方法installDecor  
```
private void installDecor() {
    mForceDecorInstall = false;
    if (mDecor == null) {
        //1.构建DecorView 
        mDecor = generateDecor(-1);
        mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        mDecor.setIsRootNamespace(true);
        if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
            mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
        }
    } else {
        mDecor.setWindow(this);
    }
    if (mContentParent == null) {
        //2.生成mContentParent  mContentParent实际是id为ID_ANDROID_CONTENT的FrameLayout，即要添加布局的父布局。
        mContentParent = generateLayout(mDecor);
        final DecorContentParent decorContentParent = (DecorContentParent) mDecor.findViewById(
                R.id.decor_content_parent);

         //3.设置Activity标题栏相关
        if (decorContentParent != null) {
            mDecorContentParent = decorContentParent;
            mDecorContentParent.setWindowCallback(getCallback());
            if (mDecorContentParent.getTitle() == null) {
                mDecorContentParent.setWindowTitle(mTitle);
            }

            final int localFeatures = getLocalFeatures();
            for (int i = 0; i < FEATURE_MAX; i++) {
                if ((localFeatures & (1 << i)) != 0) {
                    mDecorContentParent.initFeature(i);
                }
            }

            mDecorContentParent.setUiOptions(mUiOptions);

            if ((mResourcesSetFlags & FLAG_RESOURCE_SET_ICON) != 0 ||
                    (mIconRes != 0 && !mDecorContentParent.hasIcon())) {
                mDecorContentParent.setIcon(mIconRes);
            } else if ((mResourcesSetFlags & FLAG_RESOURCE_SET_ICON) == 0 &&
                    mIconRes == 0 && !mDecorContentParent.hasIcon()) {
                mDecorContentParent.setIcon(
                        getContext().getPackageManager().getDefaultActivityIcon());
                mResourcesSetFlags |= FLAG_RESOURCE_SET_ICON_FALLBACK;
            }
            if ((mResourcesSetFlags & FLAG_RESOURCE_SET_LOGO) != 0 ||
                    (mLogoRes != 0 && !mDecorContentParent.hasLogo())) {
                mDecorContentParent.setLogo(mLogoRes);
            }

            PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
            if (!isDestroyed() && (st == null || st.menu == null) && !mIsStartingWindow) {
                invalidatePanelMenu(FEATURE_ACTION_BAR);
            }
        } else {
            mTitleView = findViewById(R.id.title);
            if (mTitleView != null) {
                if ((getLocalFeatures() & (1 << FEATURE_NO_TITLE)) != 0) {
                    final View titleContainer = findViewById(R.id.title_container);
                    if (titleContainer != null) {
                        titleContainer.setVisibility(View.GONE);
                    } else {
                        mTitleView.setVisibility(View.GONE);
                    }
                    mContentParent.setForeground(null);
                } else {
                    mTitleView.setText(mTitle);
                }
            }
        }
        //4.设置背景
        if (mDecor.getBackground() == null && mBackgroundFallbackResource != 0) {
            mDecor.setBackgroundFallback(mBackgroundFallbackResource);
        }

        // 5.获取过渡元素相关信息
        if (hasFeature(FEATURE_ACTIVITY_TRANSITIONS)) {
            // ·······省略部分代码·······
        }
    }
}
```  
installDecor()主要有四个部分  
1. 构建DecorView  
2. 构建Layout并获取mContentParent对象，要添加的布局的直接父布局  
3. 设置Activity标题栏相关  
4. 设置DecorView背景  
5. 获取过渡动画信息  
  
至此，DecoverView构建完成，并将布局添加入DecoverView中    
## DecoverView什么时候添加到Window中呢  
我们知道，在Activity生命周期，onStart方法在视图可见时触发，那么DecoverView添加到window肯定在onStart方法之前   
我们一步一步往前找会发现  
Activity.onStart <- Instrumentation.callActivityOnStart <- Activity.performStart <- Activity.performRestart <- Activity.performResume <- ActivityThread.performResumeActivity <- ActivityThread.handleResumeActivity  
我们从handleResumeActivity分析  

<font size="2">(有兴趣的可以继续向前查找  Activity.startActivityForResult -> Instrumentation.execStartActivity -> ActivityManagerService.startActivity -> ActivityManagerService.startActivityAsUser -> ActivityStarter.execute -> ActivityStarter.startActivity -> ActivityStarter.startActivityUnchecked-> ActivityStack.ensureActivitiesVisibleLocked -> ActivityStack.makeVisibleAndRestartIfNeeded -> ActivityStackSupervisor.startSpecificActivityLocked -> ActivityStackSupervisor.realStartActivityLocked -> ClientLifecycleManager.scheduleTransaction -> ApplicationThread.scheduleTransaction ->  ActivityThread.scheduleTransaction ->  Activity.sendMessage(ActivityThread.H.EXECUTE_TRANSACTION, transaction) -> ActivityThread.H.handleMessage case:EXECUTE_TRANSACTION -> TransactionExecutor.execute -> TransactionExecutor.executeLifecycleState -> ResumeActivityItem.execute -> ActivityThread.handleResumeActivity )
</font><br />


```
@Override
public void handleResumeActivity(IBinder token, boolean finalStateRequest, boolean isForward,
        String reason) {
    // 取消空闲状态的 GC任务
    unscheduleGcIdler();
    mSomeActivitiesChanged = true;
    // 1.执行Activity的resume方法
    final ActivityClientRecord r = performResumeActivity(token, finalStateRequest, reason);
    // ·······省略部分代码·······
    if (r.window == null && !a.mFinished && willBeVisible) {
        r.window = r.activity.getWindow();
        View decor = r.window.getDecorView();
        // 2.将decorView设置为INVISIBLE
        decor.setVisibility(View.INVISIBLE);
        ViewManager wm = a.getWindowManager();
        WindowManager.LayoutParams l = r.window.getAttributes();
        a.mDecor = decor;
        l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
        l.softInputMode |= forwardBit;
        if (r.mPreserveWindow) {
            a.mWindowAdded = true;
            r.mPreserveWindow = false;
            // Normally the ViewRoot sets up callbacks with the Activity
            // in addView->ViewRootImpl#setView. If we are instead reusing
            // the decor view we have to notify the view root that the
            // callbacks may have changed.
            ViewRootImpl impl = decor.getViewRootImpl();
            if (impl != null) {
                impl.notifyChildRebuilt();
            }
        }
        //3. 将decor添加到WindowManager中
        if (a.mVisibleFromClient) {
            if (!a.mWindowAdded) {
                a.mWindowAdded = true;
                wm.addView(decor, l);
            } else {
                a.onWindowAttributesChanged(l);
            }
        }
    } else if (!willBeVisible) {
        if (localLOGV) Slog.v(TAG, "Launch " + r + " mStartedActivity set");
        r.hideForNow = true;
    }
    
    // ·······省略部分代码·······
    if (!r.activity.mFinished && willBeVisible && r.activity.mDecor != null && !r.hideForNow) {
        // ·······省略部分代码·······
        r.activity.mVisibleFromServer = true;
        mNumVisibleActivities++;
        if (r.activity.mVisibleFromClient) {
            //4. 设置Activity可见
            r.activity.makeVisible();
        }
    }
    
    r.nextIdle = mNewActivities;
    mNewActivities = r;
    // 添加空闲任务
    Looper.myQueue().addIdleHandler(new Idler());
}

```   
主要包含三部分  
1. 执行Activity的onResume方法  
2. 如果r.window==null 将DecorView设置为INVISIBLE 并将DecorView添加到WindowManager 
3. 设置Activity可见里面也会调用wm.addView(decorView)  

## WindowManager.addView到底做了什么事  
wm.addView的方法实现在WindowManagerImpl中，然后又调用了代理类WindowManagerGlobal的addView方法，下面看下WindowManagerGlobal.addView方法  

```
 public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
        WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
        // ·······省略部分代码·······
        ViewRootImpl root;
        View panelParentView = null;
        synchronized (mLock) {
            //1. 添加系统属性更改回调，发送变化时，调用ViewRootImpl的loadSystemProperties方法
            if (mSystemPropertyUpdater == null) {
                mSystemPropertyUpdater = new Runnable() {
                    @Override public void run() {
                        synchronized (mLock) {
                            for (int i = mRoots.size() - 1; i >= 0; --i) {
                                mRoots.get(i).loadSystemProperties();
                            }
                        }
                    }
                };
                SystemProperties.addChangeCallback(mSystemPropertyUpdater);
            }
            
            //2.判断该view是否已被添加，如果被添加过，并且在正在销毁的列表中，则进行销毁，否则抛出非法状态异常
            int index = findViewLocked(view, false);
            if (index >= 0) {
                if (mDyingViews.contains(view)) {
                    mRoots.get(index).doDie();
                } else {
                    throw new IllegalStateException("View " + view
                            + " has already been added to the window manager.");
                }
            }

            // ·······省略部分代码·······
            //3.创建ViewRootImpl对象
            root = new ViewRootImpl(view.getContext(), display);

            view.setLayoutParams(wparams);

            mViews.add(view);
            mRoots.add(root);
            mParams.add(wparams);

            // 4.调用ViewRootImpl的setView方法  
            try {
                root.setView(view, wparams, panelParentView);
            } catch (RuntimeException e) {
                // BadTokenException or InvalidDisplayException, clean up.
                if (index >= 0) {
                    removeViewLocked(index, true);
                }
                throw e;
            }
        }
    }
```
主要有四步：  
1. 添加系统属性更改回调，发送变化时，调用ViewRootImpl的loadSystemProperties方法  
2. 判断该view是否已被添加，如果被添加过，并且在正在销毁的列表中，则进行销毁，否则抛出非法状态异常  
3. 创建ViewRootImpl对象  
4. 调用ViewRootImpl的setView方法  
从源码可以看出，最终调用了ViewRootImpl.setView方法  

## 不是View却实现ViewParent的顶部视图ViewRootImpl   
从ViewParent注释可以看出，视图的顶部，协调View和WindowManager，很多程度上实现了WindowManagerGlobal的细节。  
我们从setView开始看  
```
  public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
        synchronized (this) {
            if (mView == null) {
                mView = view;
                //向DisplayManager注册显示屏监听器  可以监听到显示屏的打开和关闭
                mAttachInfo.mDisplayState = mDisplay.getState();
                mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);

                mViewLayoutDirectionInitial = mView.getRawLayoutDirection();

                // 手机反馈事件处理
                mFallbackEventHandler.setView(view);
                mWindowAttributes.copyFrom(attrs);
                if (mWindowAttributes.packageName == null) {
                    mWindowAttributes.packageName = mBasePackageName;
                }
               // ·······省略部分代码·······
                //创建mSurfaceHolder
                if (view instanceof RootViewSurfaceTaker) {
                    mSurfaceHolderCallback =
                            ((RootViewSurfaceTaker)view).willYouTakeTheSurface();
                    if (mSurfaceHolderCallback != null) {
                        mSurfaceHolder = new TakenSurfaceHolder();
                        mSurfaceHolder.setFormat(PixelFormat.UNKNOWN);
                        mSurfaceHolder.addCallback(mSurfaceHolderCallback);
                    }
                }

                // ·······省略部分代码·······
             
                // 1. 在添加到windowManager之前 安排一次布局  ，以确保我们在从系统接收任何其他事件之前进行重新布局。
                requestLayout();
                if ((mWindowAttributes.inputFeatures
                        & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
                    mInputChannel = new InputChannel();
                }
                mForceDecorViewVisibility = (mWindowAttributes.privateFlags
                        & PRIVATE_FLAG_FORCE_DECOR_VIEW_VISIBILITY) != 0;
                try {
                    mOrigWindowType = mWindowAttributes.type;
                    mAttachInfo.mRecomputeGlobalAttributes = true;
                    collectViewAttributes();
                    // 2.通过IPC将window添加入WindowSeession进行显示
                    res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                            getHostVisibility(), mDisplay.getDisplayId(), mWinFrame,
                            mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                            mAttachInfo.mOutsets, mAttachInfo.mDisplayCutout, mInputChannel);
                } 
                // ·······省略部分代码·······

                if (mInputChannel != null) {
                    if (mInputQueueCallback != null) {
                        mInputQueue = new InputQueue();
                        mInputQueueCallback.onInputQueueCreated(mInputQueue);
                    }
                    // 3. 创建窗口输入事件接收器,并设置输入管道流
                    mInputEventReceiver = new WindowInputEventReceiver(mInputChannel,
                            Looper.myLooper());
                }
                // 4. 将ViewRootImpl绑定为DecorView的Parent
                view.assignParent(this);

                // ·······省略部分代码·······
                // 5. 通过责任链模式处理输入阶段 主要针对触摸事件和物理按键事件
                mSyntheticInputStage = new SyntheticInputStage();
                InputStage viewPostImeStage = new ViewPostImeInputStage(mSyntheticInputStage);
                InputStage nativePostImeStage = new NativePostImeInputStage(viewPostImeStage,
                        "aq:native-post-ime:" + counterSuffix);
                InputStage earlyPostImeStage = new EarlyPostImeInputStage(nativePostImeStage);
                InputStage imeStage = new ImeInputStage(earlyPostImeStage,
                        "aq:ime:" + counterSuffix);
                InputStage viewPreImeStage = new ViewPreImeInputStage(imeStage);
                InputStage nativePreImeStage = new NativePreImeInputStage(viewPreImeStage,
                        "aq:native-pre-ime:" + counterSuffix);

                mFirstInputStage = nativePreImeStage;
                mFirstPostImeInputStage = earlyPostImeStage;
                mPendingInputEventQueueLengthCounterName = "aq:pending:" + counterSuffix;
            }
        }
    }
```  
把代码主要分为了五步  
1. requestLayout()在添加到windowManager之前 安排一次布局，以确保我们在从系统接收任何其他事件之前进行重新布局。  
2. 通过IPC将window添加入WindowSeession进行显示  
3. 创建窗口输入事件接收器,并设置输入管道流  
4. 将ViewRootImpl绑定为DecorView的Parent  
5. 通过责任链模式处理输入阶段 主要针对触摸事件和物理按键事件   

看下requestLayout方法  

```
 @Override
    public void requestLayout() {
        if (!mHandlingLayoutInLayoutRequest) {
            checkThread();
            mLayoutRequested = true;
            scheduleTraversals();
        }
    }
``` 
代码很简单，检查线程，执行scheduleTraversals  
检查线程会判断当前线程是否为UI线程，如果不是UI线程，则抛出异常，这里就是为什么子线程不能更新UI的原因。  
但是子线程真的不能更新UI吗？从上面分析我们可以看出，当在wm.addView时，才会创建的ViewRootImpl,而wm.addView，是在onResume时执行，所以如果在onResume执行前，是可以在子线程更新UI。(其实这个时候视图并没有被添加到view中)    
  
接下来看下scheduleTraversals方法  
```
    void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            // 在MessageQueue中添加栅栏
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            // 由编舞者处理  布局和绘制 
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
            if (!mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            // 通知渲染层将有新的帧
            notifyRendererOfFramePending();
            // 如果需要释放绘制锁
            pokeDrawLockIfNeeded();
        }
    }

```   








setContentView
phoneWindow.setContentView
installDecor();   
mDecor=generateDecore(-1) 
mContentParent=generateLayout(mDecor) 
 mLayoutInflater.inflate(layoutResID, mContentParent);
 mContentParent.requestApplyInsets();

 ActivityThread.handlerResumeActivity

 performResumeActivity  r.activity.performResume
 wm.addView(decor, l);
 r.activity.makeVisible();

 wm.addView  -> mGlobal.addView
 new ViewRootImpl(view.getContext(), display);
 root.setView(view, wparams, panelParentView);
 mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);
   res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                            getHostVisibility(), mDisplay.getDisplayId(), mWinFrame,
                            mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                            mAttachInfo.mOutsets, mAttachInfo.mDisplayCutout, mInputChannel);
mInputEventReceiver = new WindowInputEventReceiver(mInputChannel,
                            Looper.myLooper());

 scheduleTraversals()

  mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
postCallbackDelayedInternal
doScheduleCallback
scheduleFrameLocked(now)
 doFrame(System.nanoTime(), 0);

mTraversalRunnable.run
doTraversal()
 performTraversals();