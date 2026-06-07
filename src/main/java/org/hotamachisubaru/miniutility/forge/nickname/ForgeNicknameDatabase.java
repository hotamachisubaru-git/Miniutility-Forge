package org.hotamachisubaru.miniutility.forge.nickname;

import org.hotamachisubaru.miniutility.MiniutilityForge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ForgeNicknameDatabase {
    private final String dbUrl;

    public ForgeNicknameDatabase(Path databasePath) {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            MiniutilityForge.LOGGER.warn("ニックネームDBディレクトリの作成に失敗しました: {}", exception.getMessage());
        }

        this.dbUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    public void initialize() {
        try (Connection connection = DriverManager.getConnection(this.dbUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS nicknames (" +
                    "uuid TEXT PRIMARY KEY," +
                    "nickname TEXT NOT NULL" +
                    ")");
        } catch (SQLException exception) {
            MiniutilityForge.LOGGER.warn("ニックネームDBの初期化に失敗しました: {}", exception.getMessage());
        }
    }

    public void saveNickname(UUID uniqueId, String nickname) {
        if (uniqueId == null || nickname == null) {
            return;
        }

        initialize();
        try (Connection connection = DriverManager.getConnection(this.dbUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT OR REPLACE INTO nicknames (uuid, nickname) VALUES (?, ?)")) {
            statement.setString(1, uniqueId.toString());
            statement.setString(2, nickname);
            statement.executeUpdate();
        } catch (SQLException exception) {
            MiniutilityForge.LOGGER.warn("ニックネームの保存に失敗しました: {}", exception.getMessage());
        }
    }

    public void deleteNickname(UUID uniqueId) {
        if (uniqueId == null) {
            return;
        }

        initialize();
        try (Connection connection = DriverManager.getConnection(this.dbUrl);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM nicknames WHERE uuid = ?")) {
            statement.setString(1, uniqueId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            MiniutilityForge.LOGGER.warn("ニックネームの削除に失敗しました: {}", exception.getMessage());
        }
    }

    public Map<UUID, String> loadAll() {
        initialize();

        Map<UUID, String> loaded = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(this.dbUrl);
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, nickname FROM nicknames");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                try {
                    loaded.put(UUID.fromString(uuid), resultSet.getString("nickname"));
                } catch (IllegalArgumentException exception) {
                    MiniutilityForge.LOGGER.warn("不正なUUIDのニックネームデータをスキップしました: {}", uuid);
                }
            }
        } catch (SQLException exception) {
            MiniutilityForge.LOGGER.warn("ニックネームの読み込みに失敗しました: {}", exception.getMessage());
        }

        return loaded;
    }

    public void saveAll(Map<UUID, String> nicknameMap) {
        initialize();

        String sql = "INSERT OR REPLACE INTO nicknames (uuid, nickname) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(this.dbUrl)) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Map.Entry<UUID, String> entry : nicknameMap.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setString(2, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            }
        } catch (SQLException exception) {
            MiniutilityForge.LOGGER.warn("ニックネームの一括保存に失敗しました: {}", exception.getMessage());
        }
    }
}
