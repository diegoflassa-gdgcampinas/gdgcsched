/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gdg_campinas.treffen.server.schedule.model;

import static org.gdg_campinas.treffen.server.schedule.model.DataModelHelper.get;
import static org.gdg_campinas.treffen.server.schedule.model.DataModelHelper.set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.gdg_campinas.treffen.server.schedule.Config;
import org.gdg_campinas.treffen.server.schedule.model.validator.Converters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Encapsulation of the rules that maps Vendor data sources to the IOSched data sources.
 *
 */
public class DataExtractor {

  public static final String TRACK = "TRACK";
  public static final String ANDROID_TRACK = "ANDROID";
  public static final String ANDROID_TRACK_COLOR = "#AED581";
  public static final String MOBILEWEB_TRACK = "MOBILEWEB";
  public static final String MOBILEWEB_TRACK_COLOR = "#FFF176";
  public static final String CLOUD_TRACK = "CLOUD";
  public static final String DESIGN_TRACK = "DESIGN";
  public static final String FIREBASE_TRACK = "FIREBASE";
  public static final String GAMES_TRACK = "GAMES";
  public static final String IOT_TRACK = "IOT";
  public static final String LOCATION_AND_MAPS_TRACK = "LOCATION&MAPS";
  public static final String PLAY_TRACK = "PLAY";
  public static final String SEARCH_TRACK = "SEARCH";
  public static final String TV_AND_LIVINGROOM_TRACK = "TV&LIVINGROOM";
  public static final String VR_TRACK = "VR";
  public static final String MISC_TRACK = "MISC";
  public static final String ANDROIDSTUDIO_TRACK = "ANDROIDSTUDIO";
  public static final String AUTO_TRACK = "AUTO";
  public static final String MONETIZATION_TRACK = "MONETIZATION";
  public static final String WEAR_TRACK = "WEAR";
  public static final String CLOUD_TRACK_COLOR = "#80CBC4";
  public static final String DESIGN_TRACK_COLOR = "#F8BBD0";
  public static final String FIREBASE_TRACK_COLOR = "#FFD54F";
  public static final String GAMES_TRACK_COLOR = "#DCE775";
  public static final String IOT_TRACK_COLOR = "#BCAAA4";
  public static final String LOCATION_AND_MAPS_TRACK_COLOR = "#EF9A9A";
  public static final String PLAY_TRACK_COLOR = "#CE93D8";
  public static final String SEARCH_TRACK_COLOR = "#90CAF9";
  public static final String TV_AND_LIVINGROOM_TRACK_COLOR = "#B3E5FC";
  public static final String VR_TRACK_COLOR = "#FF8A65";
  public static final String MISC_TRACK_COLOR = "#C5C9E9";
  public static final String ANDROIDSTUDIO_TRACK_COLOR = "#C4E2A2";
  public static final String AUTO_TRACK_COLOR = "#CFD8DC";
  public static final String MONETIZATION_TRACK_COLOR = "#A4D7A5";
  public static final String WEAR_TRACK_COLOR = "#FFCD7A";
  public static final String ADS_TRACK = "ADS";
  public static final String ADS_TRACK_COLOR = "#B0BEC5";
  private HashMap<String, JsonObject> videoSessionsById;
  private HashMap<String, JsonObject> speakersById;
  private HashMap<String, JsonObject> categoryToTagMap;
  private HashSet<String> usedSpeakers, usedTags;
  private JsonElement mainCategory;
  private boolean obfuscate;

  public DataExtractor(boolean obfuscate) {
    this.obfuscate = obfuscate;
  }

  public JsonObject extractFromDataSources(JsonDataSources sources) {
    usedTags = new HashSet<>();
    usedSpeakers = new HashSet<>();

    JsonObject result = new JsonObject();
    result.add(OutputJsonKeys.MainTypes.rooms.name(), extractRooms(sources));
    JsonArray speakers = extractSpeakers(sources);

    JsonArray tags = extractTags(sources);
    result.add(OutputJsonKeys.MainTypes.video_library.name(), extractVideoSessions(sources));

    result.add(OutputJsonKeys.MainTypes.sessions.name(), extractSessions(sources));

    // Remove tags that are not used on any session (b/14419126)
    Iterator<JsonElement> tagsIt = tags.iterator();
    while (tagsIt.hasNext()) {
      JsonElement tag = tagsIt.next();
      String tagName = DataModelHelper.get(tag.getAsJsonObject(), OutputJsonKeys.Tags.tag).getAsString();
      if (!usedTags.contains(tagName)) {
        tagsIt.remove();
      }
    }

    // Remove speakers that are not used on any session:
    Iterator<JsonElement> it = speakers.iterator();
    while (it.hasNext()) {
      JsonElement el = it.next();
      String id = DataModelHelper.get(el.getAsJsonObject(), OutputJsonKeys.Speakers.id).getAsString();
      if (!usedSpeakers.contains(id)) {
        it.remove();
      }
    }

    result.add(OutputJsonKeys.MainTypes.speakers.name(), speakers);
    result.add(OutputJsonKeys.MainTypes.tags.name(), tags);
    return result;
  }

  public JsonArray extractRooms(JsonDataSources sources) {
    HashSet<String> ids = new HashSet<>();
    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(InputJsonKeys.VendorAPISource.MainTypes.rooms.name());
    if (source != null) {
      for (JsonObject origin: source) {
        JsonObject dest = new JsonObject();
        JsonElement originalId = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Rooms.Id);
        String id = Config.ROOM_MAPPING.getRoomId(originalId.getAsString());
        if (!ids.contains(id)) {
          String title = Config.ROOM_MAPPING.getTitle(id, DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Rooms.Name).getAsString());
          int capacity = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Rooms.Capacity).getAsInt();
          boolean filter = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Rooms.Publish).getAsBoolean();
          DataModelHelper.set(new JsonPrimitive(id), dest, OutputJsonKeys.Rooms.id);
          DataModelHelper.set(new JsonPrimitive(filter), dest, OutputJsonKeys.Rooms.filter);
          DataModelHelper.set(originalId, dest, OutputJsonKeys.Rooms.original_id);
          DataModelHelper.set(new JsonPrimitive(title), dest, OutputJsonKeys.Rooms.name);
          DataModelHelper.set(new JsonPrimitive(capacity), dest, OutputJsonKeys.Rooms.capacity);
          result.add(dest);
          ids.add(id);
        }
      }
    }
    if (Config.DEBUG_FIX_DATA) {
      DebugDataExtractorHelper.changeRooms(result);
    }
    return result;
  }

  public JsonArray extractTags(JsonDataSources sources) {
    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(InputJsonKeys.VendorAPISource.MainTypes.categories.name());
    JsonDataSource tagCategoryMappingSource = sources.getSource(InputJsonKeys.ExtraSource.MainTypes
        .tag_category_mapping.name());
    JsonDataSource tagsConfSource = sources.getSource(InputJsonKeys.ExtraSource.MainTypes.tag_conf.name());

    categoryToTagMap = new HashMap<>();

    // Only for checking duplicates.
    HashSet<String> originalTagNames = new HashSet<>();

    if (source != null) {
      for (JsonObject origin: source) {
        JsonObject dest = new JsonObject();

        // set tag category, looking for parentid in the tag_category_mapping data source
        JsonElement parentId = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Categories.ParentId);

        // Ignore categories with null parents, because they are roots (tag categories).
        if (parentId != null && !parentId.getAsString().equals("")) {
          JsonElement category = null;
          if (tagCategoryMappingSource != null) {
            JsonObject categoryMapping = tagCategoryMappingSource.getElementById(parentId.getAsString());
            if (categoryMapping != null) {
              category = DataModelHelper.get(categoryMapping, InputJsonKeys.ExtraSource.CategoryTagMapping.tag_name);
              JsonPrimitive isDefault = (JsonPrimitive) DataModelHelper.get(categoryMapping, InputJsonKeys.ExtraSource.CategoryTagMapping.is_default);
              if ( isDefault != null && isDefault.getAsBoolean() ) {
                mainCategory = category;
              }
            }
            DataModelHelper.set(category, dest, OutputJsonKeys.Tags.category);
          }

          // Ignore categories unrecognized parents (no category)
          if (category == null) {
            continue;
          }

          // Tag name is by convention: "TAGCATEGORY_TAGNAME"
          JsonElement name = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Categories.Name);
          JsonElement tagName = new JsonPrimitive(category.getAsString() + "_" +
              Converters.TAG_NAME.convert(name).getAsString());
          JsonElement originalTagName = tagName;

          if (obfuscate) {
            name = Converters.OBFUSCATE.convert(name);
            tagName = new JsonPrimitive(category.getAsString() + "_" +
                Converters.TAG_NAME.convert(name).getAsString());
          }

          DataModelHelper.set(tagName, dest, OutputJsonKeys.Tags.tag);
          DataModelHelper.set(name, dest, OutputJsonKeys.Tags.name);
          DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Categories.Id, dest, OutputJsonKeys.Tags.original_id);
          DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Categories.Description, dest, OutputJsonKeys.Tags._abstract, obfuscate ? Converters.OBFUSCATE : null);

          if (tagsConfSource != null) {
            JsonObject tagConf = tagsConfSource.getElementById(originalTagName.getAsString());
            if (tagConf != null) {
              DataModelHelper.set(tagConf, InputJsonKeys.ExtraSource.TagConf.order_in_category, dest, OutputJsonKeys.Tags.order_in_category);
              DataModelHelper.set(tagConf, InputJsonKeys.ExtraSource.TagConf.color, dest, OutputJsonKeys.Tags.color);
              DataModelHelper.set(tagConf, InputJsonKeys.ExtraSource.TagConf.hashtag, dest, OutputJsonKeys.Tags.hashtag);
            }
          }

          // TODO: Directly associate Track images with Tags.
          // Track images were retrieved from session images. This required that sessions on the
          // same track have the same image. Thus here, we needed to check the Track of each
          // session even if the corresponding Track was already assigned an image. If all sessions
          // in the same track are going to have the same image then it would make more sense for
          // the images to be attached to the Tag/Track rather than the session.
          if (tagName.getAsString().startsWith(TRACK)) {
            // Add background colors for TRACK tags.
            String trackColor = getTrackColor(tagName.getAsString().substring(6));
            if (!trackColor.isEmpty()) {
              dest.addProperty(OutputJsonKeys.Tags.color.name(), trackColor);
            }
          }

          categoryToTagMap.put(DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Categories.Id).getAsString(), dest);
          if (originalTagNames.add(originalTagName.getAsString())) {
            result.add(dest);
          }
        }
      }
    }
    if (Config.DEBUG_FIX_DATA) {
      DebugDataExtractorHelper.changeCategories(categoryToTagMap, result);
    }
    return result;
  }

  public JsonArray extractSpeakers(JsonDataSources sources) {
    speakersById = new HashMap<>();
    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(InputJsonKeys.VendorAPISource.MainTypes.speakers.name());
    if (source != null) {
      for (JsonObject origin: source) {
        JsonObject dest = new JsonObject();
        JsonElement id = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Speakers.Id);
        DataModelHelper.set(id, dest, OutputJsonKeys.Speakers.id);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Speakers.Name, dest, OutputJsonKeys.Speakers.name, obfuscate?Converters.OBFUSCATE:null);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Speakers.Bio, dest, OutputJsonKeys.Speakers.bio, obfuscate?Converters.OBFUSCATE:null);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Speakers.CompanyName, dest, OutputJsonKeys.Speakers.company, obfuscate?Converters.OBFUSCATE:null);
        JsonElement originalPhoto = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Speakers.Photo);
        if (originalPhoto != null && !"".equals(originalPhoto.getAsString())) {
          DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Speakers.Photo, dest, OutputJsonKeys.Speakers.thumbnailUrl, Converters.PHOTO_URL);
        }
        JsonElement info = origin.get(InputJsonKeys.VendorAPISource.Speakers.Info.name());
        JsonPrimitive plusUrl = DataModelHelper.getMapValue(info, InputJsonKeys.VendorAPISource.Speakers.INFO_PUBLIC_PLUS_ID, Converters.GPLUS_URL, null);
        if (plusUrl != null) {
          DataModelHelper.set(plusUrl, dest, OutputJsonKeys.Speakers.plusoneUrl);
        }
        JsonPrimitive twitter = DataModelHelper.getMapValue(info, InputJsonKeys.VendorAPISource.Speakers.INFO_PUBLIC_TWITTER, Converters.TWITTER_URL, null);
        if (twitter != null) {
          DataModelHelper.set(twitter, dest, OutputJsonKeys.Speakers.twitterUrl);
        }
        result.add(dest);
        speakersById.put(id.getAsString(), dest);
      }
    }
    return result;
  }

  public JsonArray extractSessions(JsonDataSources sources) {
    if (videoSessionsById == null) {
      throw new IllegalStateException("You need to extract video sessions before attempting to extract sessions");
    }
    if (categoryToTagMap == null) {
      throw new IllegalStateException("You need to extract tags before attempting to extract sessions");
    }

    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(InputJsonKeys.VendorAPISource.MainTypes.topics.name());
    if (source != null) {
      for (JsonObject origin: source) {
        if (isVideoSession(origin)) {
          // Sessions with the Video tag are processed as video library content
          continue;
        }
        if (isHiddenSession(origin)) {
          // Sessions with a "Hidden from schedule" flag should be ignored
          continue;
        }
        JsonElement title = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Topics.Title);
        // Since the CMS returns an empty keynote as a session, we need to ignore it
        if (title != null && title.isJsonPrimitive() && "keynote".equalsIgnoreCase(title.getAsString())) {
          continue;
        }
        JsonObject dest = new JsonObject();

        JsonElement id = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Topics.Id);
        // Some sessions require a special ID, so we replace it here...
        if (title != null && title.isJsonPrimitive() && "after hours".equalsIgnoreCase(title.getAsString())) {
          DataModelHelper.set(new JsonPrimitive("__afterhours__"), dest, OutputJsonKeys.Sessions.id);
        } else if (Arrays.asList(Config.KEYNOTE_IDS).contains(id.getAsString())) {
          // TODO: Keynotes should not have special IDs there should be a tag that identifies them
          // TODO: as keynotes and they can be handled accordingly. This check for a particular ID
          // TODO: is very brittle and should be removed.
          String keynoteId;
          if (id.getAsString().equals("3f3802e4-b24d-4b47-b9c8-b5ab7944411c")) {
            keynoteId = "__keynote__";
          } else {
            keynoteId = "__keynote2__";
          }
          DataModelHelper.set(new JsonPrimitive(keynoteId), dest,
              OutputJsonKeys.Sessions.id);

          // TODO: Keynotes should have tags like other sessions so this hack is not necessary for
          // TODO: for setting keynote colors.
          DataModelHelper.set(new JsonPrimitive("#27e4fd"), dest, OutputJsonKeys.Sessions.color);
        } else {
          DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Topics.Id, dest, OutputJsonKeys.Sessions.id);
        }
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Topics.Id, dest, OutputJsonKeys.Sessions.url, Converters.SESSION_URL);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Topics.Title, dest, OutputJsonKeys.Sessions.title, obfuscate?Converters.OBFUSCATE:null);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Topics.Description, dest, OutputJsonKeys.Sessions.description, obfuscate?Converters.OBFUSCATE:null);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Topics.Start, dest, OutputJsonKeys.Sessions.startTimestamp, Converters.DATETIME);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Topics.Finish, dest, OutputJsonKeys.Sessions.endTimestamp, Converters.DATETIME);
        DataModelHelper.set(new JsonPrimitive(isFeatured(origin)), dest, OutputJsonKeys.Sessions.isFeatured);

        setVideoPropertiesInSession(origin, dest);
        setRelatedContent(origin, dest);

        JsonElement mainTag = null;
        JsonElement hashtag = null;
        JsonElement mainTagColor = null;
        JsonArray categories= origin.getAsJsonArray(InputJsonKeys.VendorAPISource.Topics.CategoryIds.name());
        JsonArray tags = new JsonArray();
        for (JsonElement category: categories) {
          JsonObject tag = categoryToTagMap.get(category.getAsString());
          if (tag != null) {
            JsonElement tagName = DataModelHelper.get(tag, OutputJsonKeys.Tags.tag);
            tags.add(tagName);
            usedTags.add(tagName.getAsString());

            if (mainTag == null) {
              // check if the tag is from a "default" category. For example, if THEME is the default
              // category, all sessions will have a "mainTag" property set to the first tag of type THEME
              JsonElement tagCategory = DataModelHelper.get(tag, OutputJsonKeys.Tags.category); // THEME, TYPE or TOPIC
              if (tagCategory.equals(mainCategory)) {
                mainTag = tagName;
                mainTagColor = DataModelHelper.get(tag, OutputJsonKeys.Tags.color);
              }
              if (hashtag == null && DataModelHelper.isHashtag(tag)) {
                hashtag = DataModelHelper.get(tag, OutputJsonKeys.Tags.hashtag);
                if (hashtag == null || hashtag.getAsString() == null || hashtag.getAsString().isEmpty()) {
                  // If no hashtag set in the tagsconf file, we will convert the tagname to find one:
                  hashtag = new JsonPrimitive(DataModelHelper.get(tag, OutputJsonKeys.Tags.name, Converters.TAG_NAME)
                          .getAsString().toLowerCase());
                }
              }
            }
          }
        }
        // TODO: Keynotes should have their own tags that identify them. Adding here like this
        // TODO: should not be necessary.
        if (DataModelHelper.get(dest, OutputJsonKeys.Sessions.id).getAsString().startsWith("__keynote")) {
          tags.add("FLAG_KEYNOTE");
        }
        DataModelHelper.set(tags, dest, OutputJsonKeys.Sessions.tags);
        if (mainTag != null) {
          DataModelHelper.set(mainTag, dest, OutputJsonKeys.Sessions.mainTag);
        }
        if (mainTagColor != null) {
          DataModelHelper.set(mainTagColor, dest, OutputJsonKeys.Sessions.color);
        }
        if (hashtag != null) {
          DataModelHelper.set(hashtag, dest, OutputJsonKeys.Sessions.hashtag);
        }

        JsonArray speakers = DataModelHelper.getAsArray(origin, InputJsonKeys.VendorAPISource.Topics.SpeakerIds);
        if (speakers != null) for (JsonElement speaker: speakers) {
            String speakerId = speaker.getAsString();
            usedSpeakers.add(speakerId);
        }
        DataModelHelper.set(speakers, dest, OutputJsonKeys.Sessions.speakers);

        JsonArray sessions= origin.getAsJsonArray(InputJsonKeys.VendorAPISource.Topics.Sessions.name());
        if (sessions != null && sessions.size()>0) {
          String roomId = DataModelHelper.get(sessions.get(0).getAsJsonObject(), InputJsonKeys.VendorAPISource.Sessions.RoomId).getAsString();
          roomId = Config.ROOM_MAPPING.getRoomId(roomId);
          DataModelHelper.set(new JsonPrimitive(roomId), dest, OutputJsonKeys.Sessions.room);

          // captions URL is set based on the session room, so keep it here.
          String captionsURL = Config.ROOM_MAPPING.getCaptions(roomId);
          if (captionsURL != null) {
            DataModelHelper.set(new JsonPrimitive(captionsURL), dest, OutputJsonKeys.Sessions.captionsUrl);
          }
        }

        if (Config.DEBUG_FIX_DATA) {
          DebugDataExtractorHelper.changeSession(dest, usedTags);
        }
        result.add(dest);
      }
    }
    return result;
  }

  public JsonArray extractVideoSessions(JsonDataSources sources) {
    videoSessionsById = new HashMap<>();
    if (categoryToTagMap == null) {
      throw new IllegalStateException("You need to extract tags before attempting to extract video sessions");
    }
    if (speakersById == null) {
      throw new IllegalStateException("You need to extract speakers before attempting to extract video sessions");
    }

    JsonArray result = new JsonArray();
    JsonDataSource source = sources.getSource(InputJsonKeys.VendorAPISource.MainTypes.topics.name());
    if (source != null) {
      for (JsonObject origin: source) {

        if (!isVideoSession(origin)) {
          continue;
        }
        if (isHiddenSession(origin)) {
          // Sessions with a "Hidden from schedule" flag should be ignored
          continue;
        }

        JsonObject dest = new JsonObject();

        JsonPrimitive vid = setVideoForVideoSession(origin, dest);

        JsonElement id = DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Topics.Id);
        // video library id must be the Youtube video id
        DataModelHelper.set(vid, dest, OutputJsonKeys.VideoLibrary.id);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Topics.Title, dest, OutputJsonKeys.VideoLibrary.title, obfuscate?Converters.OBFUSCATE:null);
        DataModelHelper.set(origin, InputJsonKeys.VendorAPISource.Topics.Description, dest, OutputJsonKeys.VideoLibrary.desc, obfuscate?Converters.OBFUSCATE:null);
        DataModelHelper.set(new JsonPrimitive(Config.CONFERENCE_YEAR), dest, OutputJsonKeys.VideoLibrary.year);


        JsonElement videoTopic = null;
        JsonArray categories= origin.getAsJsonArray(InputJsonKeys.VendorAPISource.Topics.CategoryIds.name());
        for (JsonElement category: categories) {
          JsonObject tag = categoryToTagMap.get(category.getAsString());
          if (tag != null) {
            if (DataModelHelper.isHashtag(tag)) {
              videoTopic = DataModelHelper.get(tag, OutputJsonKeys.Tags.name);
              // by definition, the first tag that can be a hashtag (usually a TOPIC) is considered the video tag
              break;
            }
          }
        }
        if (videoTopic != null) {
          DataModelHelper.set(videoTopic, dest, OutputJsonKeys.VideoLibrary.topic);
        }

        // Concatenate speakers:
        JsonArray speakers = DataModelHelper.getAsArray(origin, InputJsonKeys.VendorAPISource.Topics.SpeakerIds);
        StringBuilder sb = new StringBuilder();
        if (speakers != null) for (int i=0; i<speakers.size(); i++) {
          String speakerId = speakers.get(i).getAsString();
          usedSpeakers.add(speakerId);
          JsonObject speaker = speakersById.get(speakerId);
          if (speaker != null) {
            sb.append(DataModelHelper.get(speaker, OutputJsonKeys.Speakers.name).getAsString());
            if (i<speakers.size()-1) sb.append(", ");
          }
        }
        DataModelHelper.set(new JsonPrimitive(sb.toString()), dest, OutputJsonKeys.VideoLibrary.speakers);
        videoSessionsById.put(id.getAsString(), dest);
        result.add(dest);
      }
    }
    return result;
  }

  private boolean isVideoSession(JsonObject sessionObj) {
    JsonArray tags= sessionObj.getAsJsonArray(InputJsonKeys.VendorAPISource.Topics.CategoryIds.name());
    for (JsonElement category: tags) {
      if (Config.VIDEO_CATEGORY.equals(category.getAsString())) {
        return true;
      }
    }
    return false;
  }

  private boolean isHiddenSession(JsonObject sessionObj) {
    JsonPrimitive hide = DataModelHelper.getMapValue(
            DataModelHelper.get(sessionObj, InputJsonKeys.VendorAPISource.Topics.Info),
            InputJsonKeys.VendorAPISource.Topics.INFO_HIDDEN_SESSION,
            Converters.BOOLEAN, null);
    if (hide != null && hide.isBoolean() && hide.getAsBoolean()) {
      return true;
    }
    return false;
  }

  private boolean isLivestreamed(JsonObject sessionObj) {
    // data generated after the end of the conference should never have livestream URLs
    long endOfConference = Config.CONFERENCE_DAYS[Config.CONFERENCE_DAYS.length-1][1];
    if (System.currentTimeMillis() > endOfConference ) {
      return false;
    }
    JsonPrimitive livestream = DataModelHelper.getMapValue(
        DataModelHelper.get(sessionObj, InputJsonKeys.VendorAPISource.Topics.Info),
        InputJsonKeys.VendorAPISource.Topics.INFO_IS_LIVE_STREAM,
        null, null);
    return livestream != null && "true".equalsIgnoreCase(livestream.getAsString());
  }

  /** Check whether a session is featured or not.
   *
   * @param sessionObj Session to check
   * @return True if featured, false otherwise.
   */
  private boolean isFeatured(JsonObject sessionObj) {
    // Extract "Featured Session" flag from EventPoint "info" block.
    JsonPrimitive featured = DataModelHelper.getMapValue(
            DataModelHelper.get(sessionObj, InputJsonKeys.VendorAPISource.Topics.Info),
            InputJsonKeys.VendorAPISource.Topics.INFO_FEATURED_SESSION,
            Converters.BOOLEAN, null);
    if (featured != null && featured.isBoolean() && featured.getAsBoolean()) {
      return true;
    }
    return false;
  }

  private void setVideoPropertiesInSession(JsonObject origin, JsonObject dest) {
    boolean isLivestream = isLivestreamed(origin);
    DataModelHelper.set(new JsonPrimitive(isLivestream), dest, OutputJsonKeys.Sessions.isLivestream);

    JsonPrimitive videoUrl = getVideoFromTopicInfo(origin, InputJsonKeys.VendorAPISource.Topics.INFO_VIDEO_URL, null);
    DataModelHelper.set(videoUrl, dest, OutputJsonKeys.Sessions.youtubeUrl);
  }

  private JsonPrimitive getVideoFromTopicInfo(JsonObject origin, String sourceInfoKey, String defaultVideoUrl) {
    JsonPrimitive result = null;

    if (!obfuscate) {
      JsonPrimitive vid = DataModelHelper.getMapValue(
          DataModelHelper.get(origin, InputJsonKeys.VendorAPISource.Topics.Info), sourceInfoKey, null, defaultVideoUrl);
      if (vid != null && !vid.getAsString().isEmpty()) {
        result = vid;
      }
    }
    return (result == null && defaultVideoUrl != null) ? new JsonPrimitive(defaultVideoUrl) : result;
  }

  private JsonPrimitive  setVideoForVideoSession(JsonObject origin, JsonObject dest) {
    JsonPrimitive vid = getVideoFromTopicInfo(origin,
        InputJsonKeys.VendorAPISource.Topics.INFO_VIDEO_URL, null);
    if (vid != null) {
      DataModelHelper.set(vid, dest, OutputJsonKeys.VideoLibrary.vid);
      JsonPrimitive thumbnail = new JsonPrimitive("http://img.youtube.com/vi/" + vid.getAsString() + "/hqdefault.jpg");
      DataModelHelper.set(thumbnail, dest, OutputJsonKeys.VideoLibrary.thumbnailUrl);
    }
    return vid;
  }

  @Deprecated
  private void setRelatedVideos(JsonObject origin, JsonObject dest) {
    JsonArray related = DataModelHelper.getAsArray(origin, InputJsonKeys.VendorAPISource.Topics.Related);
    if (related == null) {
      return;
    }
    for (JsonElement el: related) {
      if (!el.isJsonObject()) {
        continue;
      }
      JsonObject obj = el.getAsJsonObject();
      if (!obj.has("name") || !obj.has("values")) {
        continue;
      }

      if (InputJsonKeys.VendorAPISource.Topics.RELATED_NAME_VIDEO.equals(
          obj.getAsJsonPrimitive("name").getAsString())) {

        JsonElement values = obj.get("values");
        if (!values.isJsonArray()) {
          continue;
        }

        // As per the data specification, related content is formatted as
        // "video1 title1\nvideo2 title2\n..."
        StringBuilder relatedContentStr = new StringBuilder();
        for (JsonElement value: values.getAsJsonArray()) {
          String relatedSessionId = value.getAsString();
          JsonObject relatedVideo = videoSessionsById.get(relatedSessionId);
          if (relatedVideo != null) {
            JsonElement vid = DataModelHelper.get(relatedVideo, OutputJsonKeys.VideoLibrary.vid);
            JsonElement title = DataModelHelper.get(relatedVideo, OutputJsonKeys.VideoLibrary.title);
            if (vid != null && title != null) {
              relatedContentStr.append(vid.getAsString()).append(" ")
                .append(title.getAsString()).append("\n");
            }
          }
        }
        DataModelHelper.set(new JsonPrimitive(relatedContentStr.toString()),
            dest, OutputJsonKeys.Sessions.relatedContent);
      }
    }
  }

  private void setRelatedContent(JsonObject origin, JsonObject dest) {
    JsonArray related = DataModelHelper.getAsArray(origin, InputJsonKeys.VendorAPISource.Topics.Related);
    JsonArray outputArray = new JsonArray();

    if (related == null) {
      return;
    }
    for (JsonElement el: related) {
      if (!el.isJsonObject()) {
        continue;
      }
      JsonObject obj = el.getAsJsonObject();
      if (!obj.has("name") || !obj.has("values")) {
        continue;
      }

      if (InputJsonKeys.VendorAPISource.Topics.RELATED_NAME_SESSIONS.equals(
              obj.getAsJsonPrimitive("name").getAsString())) {

        JsonElement values = obj.get("topics");
        if (!values.isJsonArray()) {
          continue;
        }

        // As per the data specification, related content is formatted as
        // "video1 title1\nvideo2 title2\n..."
        for (JsonElement topic : values.getAsJsonArray()) {
          if (!topic.isJsonObject()) {
            continue;
          }

          JsonObject topicObj = topic.getAsJsonObject();

          String id = DataModelHelper.get(topicObj, InputJsonKeys.VendorAPISource.RelatedTopics.Id).getAsString();
          String title = DataModelHelper.get(topicObj, InputJsonKeys.VendorAPISource.RelatedTopics.Title).getAsString();

          if (id != null && title != null) {
            JsonObject outputObj = new JsonObject();
            DataModelHelper.set(new JsonPrimitive(id), outputObj, OutputJsonKeys.RelatedContent.id);
            DataModelHelper.set(new JsonPrimitive(title), outputObj, OutputJsonKeys.RelatedContent.title);
            outputArray.add(outputObj);
          }
        }
        DataModelHelper.set(outputArray, dest, OutputJsonKeys.Sessions.relatedContent);
      }
    }
  }

  // TODO: improve the association of colors with tracks.
  // Track and corresponding track colors were hard coded. These values should be defined at best
  // as part of a config file retrieved from the application server, or at least as a config file
  // within the application.
  /**
   * Provides the appropriate color given the track name.
   *
   * @param trackName Name of the track requiring a color.
   * @return Color associated with track name.
   */
  private String getTrackColor(String trackName) {
    switch (trackName) {
      // Known tracks
      case ANDROID_TRACK:
        return ANDROID_TRACK_COLOR;
      case MOBILEWEB_TRACK:
        return MOBILEWEB_TRACK_COLOR;
      case CLOUD_TRACK:
        return CLOUD_TRACK_COLOR;
      case DESIGN_TRACK:
        return DESIGN_TRACK_COLOR;
      case FIREBASE_TRACK:
        return FIREBASE_TRACK_COLOR;
      case GAMES_TRACK:
        return GAMES_TRACK_COLOR;
      case IOT_TRACK:
        return IOT_TRACK_COLOR;
      case LOCATION_AND_MAPS_TRACK:
        return LOCATION_AND_MAPS_TRACK_COLOR;
      case PLAY_TRACK:
        return PLAY_TRACK_COLOR;
      case SEARCH_TRACK:
        return SEARCH_TRACK_COLOR;
      case TV_AND_LIVINGROOM_TRACK:
        return TV_AND_LIVINGROOM_TRACK_COLOR;
      case VR_TRACK:
        return VR_TRACK_COLOR;
      case MISC_TRACK:
        return MISC_TRACK_COLOR;
      case ADS_TRACK:
        return ADS_TRACK_COLOR;

      // other tracks
      case ANDROIDSTUDIO_TRACK:
        return ANDROIDSTUDIO_TRACK_COLOR;
      case AUTO_TRACK:
        return AUTO_TRACK_COLOR;
      case MONETIZATION_TRACK:
        return MONETIZATION_TRACK_COLOR;
      case WEAR_TRACK:
        return WEAR_TRACK_COLOR;
      default:
        return "";
    }
  }

}
