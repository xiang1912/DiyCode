package com.plusend.diycode.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.plusend.diycode.R;
import com.plusend.diycode.mvp.model.entity.TopicDetail;
import com.plusend.diycode.mvp.model.entity.TopicReply;
import com.plusend.diycode.mvp.presenter.FollowPresenter;
import com.plusend.diycode.mvp.presenter.TopicPresenter;
import com.plusend.diycode.mvp.presenter.TopicRepliesPresenter;
import com.plusend.diycode.mvp.view.FollowView;
import com.plusend.diycode.mvp.view.TopicRepliesView;
import com.plusend.diycode.mvp.view.TopicView;
import com.plusend.diycode.util.Constant;
import com.plusend.diycode.view.adapter.DividerListItemDecoration;
import com.plusend.diycode.view.adapter.topic.TopicRepliesAdapter;
import java.util.ArrayList;
import java.util.List;

public class TopicActivity extends AppCompatActivity
    implements TopicView, TopicRepliesView, FollowView {
  private static final String TAG = "TopicActivity";
  public static final String ID = "topicId";

  @BindView(R.id.rv) RecyclerView rv;
  @BindView(R.id.fab) FloatingActionButton fab;
  @BindView(R.id.toolbar) Toolbar toolbar;

  private int id;
  private TopicDetail topicDetail;
  private List<TopicReply> topicReplyList = new ArrayList<>();
  private TopicRepliesAdapter topicRepliesAdapter;
  private TopicPresenter topicPresenter;
  private TopicRepliesPresenter topicRepliesPresenter;
  private FollowPresenter followPresenter;
  private LinearLayoutManager linearLayoutManager;
  private int offset;
  private boolean noMoreReplies;

  @Override protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_topic);
    ButterKnife.bind(this);
    initActionBar(toolbar);

    Intent intent = getIntent();
    id = intent.getIntExtra(ID, 0);
    Log.d(TAG, "id: " + id);
    linearLayoutManager = new LinearLayoutManager(this);
    rv.setLayoutManager(linearLayoutManager);
    topicRepliesAdapter = new TopicRepliesAdapter(topicReplyList, null);
    rv.setAdapter(topicRepliesAdapter);
    rv.addItemDecoration(new DividerListItemDecoration(this));
    rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
      private int lastVisibleItem;

      @Override public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (!noMoreReplies
            && newState == RecyclerView.SCROLL_STATE_IDLE
            && lastVisibleItem + 1 == topicRepliesAdapter.getItemCount()) {
          topicRepliesAdapter.setStatus(TopicRepliesAdapter.STATUS_LOADING);
          topicRepliesPresenter.addReplies(offset);
        }
      }

      @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
      }
    });

    topicPresenter = new TopicPresenter(this);
    if (id != 0) {
      topicPresenter.getTopic(id);
    }

    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        Intent intent = new Intent(TopicActivity.this, CreateTopicReplyActivity.class);
        intent.putExtra(CreateTopicReplyActivity.TOPIC_ID, topicDetail.getId());
        intent.putExtra(CreateTopicReplyActivity.TOPIC_TITLE, topicDetail.getTitle());
        startActivity(intent);
      }
    });
  }

  private void initActionBar(Toolbar toolbar) {
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override public void showTopic(TopicDetail topicDetail) {
    this.topicDetail = topicDetail;
    topicRepliesAdapter.setTopicDetail(topicDetail);
    followPresenter = new FollowPresenter(this, topicDetail.getId());
    followPresenter.start();
    topicRepliesAdapter.setOnFavoriteItemClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        followPresenter.serFavorite(true);
      }
    });
    topicRepliesAdapter.notifyDataSetChanged();
    requestReplies();
  }

  private void requestReplies() {
    topicRepliesPresenter = new TopicRepliesPresenter(this, id);
    topicRepliesPresenter.start();
    topicRepliesPresenter.getReplies();
  }

  @Override public Context getContext() {
    return this;
  }

  @Override protected void onStart() {
    super.onStart();
    topicPresenter.start();
  }

  @Override protected void onStop() {
    topicPresenter.stop();
    if (topicRepliesPresenter != null) {
      topicRepliesPresenter.stop();
    }
    if (followPresenter != null) {
      followPresenter.stop();
    }
    super.onStop();
  }

  @Override public void showReplies(List<TopicReply> topicReplyList) {
    if (topicReplyList != null) {
      noReplies(topicReplyList);
      this.topicReplyList.clear();
      this.topicReplyList.addAll(topicReplyList);
      if (topicReplyList.size() == 0) {
        topicRepliesAdapter.setStatus(TopicRepliesAdapter.STATUS_NO_MORE);
      } else {
        topicRepliesAdapter.setStatus(TopicRepliesAdapter.STATUS_NORMAL);
      }
    }
    offset = this.topicReplyList.size();
    topicRepliesAdapter.notifyDataSetChanged();
  }

  @Override public void addReplies(List<TopicReply> topicReplyList) {
    if (topicReplyList != null) {
      noReplies(topicReplyList);
      this.topicReplyList.addAll(topicReplyList);
      if (topicReplyList.size() == 0) {
        topicRepliesAdapter.setStatus(TopicRepliesAdapter.STATUS_NO_MORE);
      } else {
        topicRepliesAdapter.setStatus(TopicRepliesAdapter.STATUS_NORMAL);
      }
    }
    offset = this.topicReplyList.size();
    topicRepliesAdapter.notifyDataSetChanged();
  }

  private void noReplies(List<TopicReply> topicReplyList) {
    if (topicReplyList.size() == 0) {
      noMoreReplies = true;
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        break;
      default:
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public void setFollow(boolean isFollowed) {
    topicRepliesAdapter.setFollow(isFollowed);
  }

  @Override public void setFavorite(boolean isFavorite) {
    topicRepliesAdapter.setFavorite(isFavorite);
  }
}
