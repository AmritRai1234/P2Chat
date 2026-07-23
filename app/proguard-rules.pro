# Keep Room entities and DAOs
-keep class com.p2pchat.data.model.** { *; }
-keep class com.p2pchat.data.local.** { *; }

# Keep Domain models
-keep class com.p2pchat.domain.model.** { *; }

# Keep Gson payloads
-keep class com.p2pchat.nearby.PayloadWrapper { *; }
-keep class com.p2pchat.nearby.GroupMessagePayload { *; }
-keep class com.p2pchat.nearby.GroupInvitePayload { *; }
-keep class com.p2pchat.nearby.JoinRequestPayload { *; }
-keep class com.p2pchat.nearby.JoinApprovalPayload { *; }
-keep class com.p2pchat.nearby.FileMetadataPayload { *; }

# Keep Hilt generated classes
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Keep Nearby Connections API
-keep class com.google.android.gms.nearby.** { *; }

# R8 Aggressive Bytecode Shrinking & Optimization
-optimizationpasses 5
-repackageclasses ''
-allowaccessmodification
