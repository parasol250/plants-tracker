package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class AppInterface extends Application {
	private List<PlantState> plants = new ArrayList<>();
	private VBox mainLayout;
	private FlowPane cardsLayout;
	private TextArea historyArea;
	private Timer reminderTimer;
	private ConcurrentHashMap<String, Boolean> shownReminders = new ConcurrentHashMap<>();

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Трекер ухода за растениями");

		mainLayout = new VBox(15);
		mainLayout.setPadding(new Insets(15));
		mainLayout.setAlignment(Pos.TOP_CENTER);

		Button addPlantBtn = new Button("+ Добавить растение");
		addPlantBtn.setOnAction(e -> showAddPlantDialog());

		Button addSpeciesBtn = new Button("+ Создать новый вид");
		addSpeciesBtn.setOnAction(e -> showAddSpeciesDialog());

		// Добавляем обе кнопки в верхний контейнер HBox вместо одного addPlantBtn
		HBox topButtonsBox = new HBox(15, addPlantBtn, addSpeciesBtn);
		topButtonsBox.setAlignment(Pos.CENTER);

		cardsLayout = new FlowPane();
		cardsLayout.setHgap(20);
		cardsLayout.setVgap(20);
		cardsLayout.setPadding(new Insets(10));
		cardsLayout.setAlignment(Pos.TOP_LEFT);

		ScrollPane scrollPane = new ScrollPane(cardsLayout);
		scrollPane.setFitToWidth(true);
		scrollPane.setPrefHeight(350);
		scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

		historyArea = new TextArea();
		historyArea.setEditable(false);
		historyArea.setPrefHeight(150);
		historyArea.setPromptText("История ухода будет отображаться здесь");

		mainLayout.getChildren().addAll(addPlantBtn, new Separator(), scrollPane, new Separator(), historyArea, topButtonsBox);

		// ИСПРАВЛЕНО (Ошибки 62-64): Вместо хардкода загружаем 5 реальных записей из
		// базы данных
		plants = PlantAndDB.loadAllPlants();

		refreshPlantCards();

		// Таймер для обновления интерфейса каждую секунду
		Timer updateTimer = new Timer(true);
		updateTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(() -> refreshPlantCards());
			}
		}, 0, 1000);

		startReminderChecker();

		Scene scene = new Scene(mainLayout, 900, 650);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private void startReminderChecker() {
		reminderTimer = new Timer(true);
		reminderTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				checkReminders();
			}
		}, 0, 1000);
	}

	private void checkReminders() {
		for (PlantState plant : plants) {
			checkReminderForType(plant, ProcedureType.WATERING, "полив");
			checkReminderForType(plant, ProcedureType.FERTILIZING, "удобрение");
			checkReminderForType(plant, ProcedureType.REPOTTING, "пересадку");
		}
	}

	private void checkReminderForType(PlantState plant, ProcedureType type, String typeName) {
		long secondsUntil = plant.getSecondsUntil(type);

		if (secondsUntil == 5) {
			String reminderKey = plant.getName() + "_" + typeName + "_5";
			if (!shownReminders.containsKey(reminderKey)) {
				shownReminders.put(reminderKey, true);
				Platform.runLater(() -> {
					showReminderDialog(plant, type, typeName, secondsUntil);
				});
			}
		} else if (secondsUntil > 5) {
			String reminderKey = plant.getName() + "_" + typeName + "_5";
			shownReminders.remove(reminderKey);
		}
	}

	private void showReminderDialog(PlantState plant, ProcedureType type, String typeName, long seconds) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Напоминание об уходе");
		alert.setHeaderText("Внимание! Осталось всего " + seconds + " секунд!");
		alert.setContentText("Растение: " + plant.getName() + "\n" + "Действие: " + typeName + "\n"
				+ "До планового ухода осталось: " + seconds + " секунд\n\n" + "Что хотите сделать?");

		ButtonType remindLater = new ButtonType("Напомнить позже", ButtonBar.ButtonData.OTHER);
		ButtonType markDone = new ButtonType("Отметить как выполненное", ButtonBar.ButtonData.OK_DONE);

		alert.getButtonTypes().setAll(markDone, remindLater);

		Button okButton = (Button) alert.getDialogPane().lookupButton(markDone);
		okButton.setDefaultButton(true);

		alert.showAndWait().ifPresent(response -> {
			String reminderKey = plant.getName() + "_" + typeName + "_5";
			if (response == markDone) {
				// ИСПРАВЛЕНО: Сохраняем событие ухода намертво в таблицу CareHistory через JDBC
				PlantAndDB.saveCareAction(plant, type);

				String timestamp = java.time.LocalDateTime.now()
						.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
				historyArea.appendText(
						"[" + timestamp + "] " + plant.getName() + ": " + typeName + " выполнен через напоминание \n");

				shownReminders.remove(reminderKey);
				refreshPlantCards();

				Alert confirmAlert = new Alert(Alert.AlertType.INFORMATION);
				confirmAlert.setTitle("Успех");
				confirmAlert.setHeaderText(null);
				confirmAlert.setContentText("Уход за растением \"" + plant.getName() + "\" успешно зафиксирован в БД!");
				confirmAlert.showAndWait();
			} else if (response == remindLater) {
				shownReminders.remove(reminderKey);
				Alert laterAlert = new Alert(Alert.AlertType.INFORMATION);
				laterAlert.setTitle("Напоминание отложено");
				laterAlert.setHeaderText(null);
				laterAlert.setContentText("Напоминание появится снова через 5 секунд.");
				laterAlert.showAndWait();
			}
		});
	}

	private void refreshPlantCards() {
		cardsLayout.getChildren().clear();

		for (PlantState plant : plants) {
			VBox card = new VBox(10);
			card.setAlignment(Pos.TOP_CENTER);
			card.setPadding(new Insets(12));
			card.setStyle(
					"-fx-border-color: #b5b5b5; -fx-border-radius: 5; -fx-background-color: #ffffff; -fx-background-radius: 5;");
			card.setPrefWidth(260);

			HBox topButtons = new HBox(8);
			topButtons.setAlignment(Pos.CENTER);
			Button changePhotoBtn = new Button("Фото");
			Button deleteBtn = new Button("Удалить");
			Button historyBtn = new Button("История");
			Button editBtn = new Button("Ред.");

			historyBtn.setStyle("-fx-font-size: 10px;");
			changePhotoBtn.setStyle("-fx-font-size: 10px;");
			deleteBtn.setStyle("-fx-font-size: 10px;");
			editBtn.setStyle("-fx-font-size: 10px;");

			ImageView imageView = new ImageView();
			imageView.setFitWidth(100);
			imageView.setFitHeight(100);
			imageView.setPreserveRatio(true);

			if (plant.getPhotoPath() != null && !plant.getPhotoPath().isEmpty()) {
				try {
					Image img = new Image(new File(plant.getPhotoPath()).toURI().toString());
					imageView.setImage(img);
				} catch (Exception e) {
					setDefaultImage(imageView);
				}
			} else {
				setDefaultImage(imageView);
			}

			changePhotoBtn.setOnAction(e -> {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Выберите изображение");
				fileChooser.getExtensionFilters()
						.addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
				Stage stage = (Stage) changePhotoBtn.getScene().getWindow();
				File selectedFile = fileChooser.showOpenDialog(stage);
				if (selectedFile != null) {
					Image image = new Image(selectedFile.toURI().toString());
					imageView.setImage(image);
					plant.setPhotoPath(selectedFile.getAbsolutePath());

					// Сохраняем путь в реляционную таблицу через JDBC
					PlantAndDB.updatePlantPhoto(plant.getId(), selectedFile.getAbsolutePath());
				}
			});

			deleteBtn.setOnAction(e -> {
				PlantAndDB.deletePlant(plant.getId());
				plants.remove(plant);
				refreshPlantCards();
			});

			historyBtn.setOnAction(e -> showPlantHistory(plant));
			editBtn.setOnAction(e -> editPlant(plant));

			topButtons.getChildren().addAll(changePhotoBtn, editBtn, deleteBtn, historyBtn);

			// К имени цветка добавляем название его вида из БД для наглядности
			Label nameLabel = new Label(plant.getName() + " (" + plant.getSpeciesName() + ")");
			nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

			GridPane statusGrid = new GridPane();
			statusGrid.setHgap(8);
			statusGrid.setVgap(6);
			statusGrid.setAlignment(Pos.CENTER);

			ProcedureType[] types = { ProcedureType.WATERING, ProcedureType.FERTILIZING, ProcedureType.REPOTTING };
			String[] labels = { "Полив", "Удобрение", "Пересадка" };
			String[] cbLabels = { "Полито", "Удобрено", "Пересажено" };

			for (int i = 0; i < types.length; i++) {
				ProcedureType type = types[i];

				javafx.scene.shape.Circle indicator = new javafx.scene.shape.Circle(6);
				CareStatus status = plant.getCareStatus(type);
				if (status == CareStatus.GREEN) {
					indicator.setFill(javafx.scene.paint.Color.GREEN);
				} else if (status == CareStatus.YELLOW) {
					indicator.setFill(javafx.scene.paint.Color.GOLD);
				} else {
					indicator.setFill(javafx.scene.paint.Color.RED);
				}
				Label procLabel = new Label(labels[i]);
				procLabel.setStyle("-fx-font-size: 11px;");
				long secondsUntil = plant.getSecondsUntil(type);
				String timeText;
				if (secondsUntil >= 86400) {
					timeText = (secondsUntil / 86400) + "д " + ((secondsUntil % 86400) / 3600) + "ч";
				} else if (secondsUntil >= 3600) {
					timeText = (secondsUntil / 3600) + "ч " + ((secondsUntil % 3600) / 60) + "м";
				} else if (secondsUntil >= 60) {
					timeText = (secondsUntil / 60) + "м " + (secondsUntil % 60) + "с";
				} else {
					timeText = secondsUntil + "с";
				}
				Label secondsLabel = new Label(timeText);
				secondsLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: gray;");
				CheckBox checkBox = new CheckBox(cbLabels[i]);
				checkBox.setStyle("-fx-font-size: 11px;");
				checkBox.setSelected(false);
				checkBox.setOnAction(e -> {
					if (checkBox.isSelected()) {
// ИСПРАВЛЕНО: При клике на чекбокс данные мгновенно улетают в реляционные таблицы
						PlantAndDB.saveCareAction(plant, type);
						java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter
								.ofPattern("dd.MM.yyyy HH:mm:ss");
						historyArea.appendText("(" + java.time.LocalDateTime.now().format(dtf) + ") " + plant.getName()
								+ ": " + procLabel.getText() + " выполнен \n");
						refreshPlantCards();
					}
				});
				statusGrid.add(indicator, 0, i);
				statusGrid.add(procLabel, 1, i);
				statusGrid.add(secondsLabel, 2, i);
				statusGrid.add(checkBox, 3, i);
			}
			VBox nextCareBox = new VBox(2);
			nextCareBox.setAlignment(Pos.CENTER_LEFT);
			nextCareBox.setPadding(new Insets(5, 0, 0, 10));
			Label nextCareTitle = new Label("Ближайший уход:");
			nextCareTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
			long minSeconds = Math.min(plant.getSecondsUntil(ProcedureType.WATERING), Math.min(
					plant.getSecondsUntil(ProcedureType.FERTILIZING), plant.getSecondsUntil(ProcedureType.REPOTTING)));
			String timeText;
			if (minSeconds >= 86400) {
				timeText = (minSeconds / 86400) + " дн " + ((minSeconds % 86400) / 3600) + " ч";
			} else if (minSeconds >= 3600) {
				timeText = (minSeconds / 3600) + " ч " + ((minSeconds % 3600) / 60) + " мин";
			} else if (minSeconds >= 60) {
				timeText = (minSeconds / 60) + " мин " + (minSeconds % 60) + " сек";
			} else {
				timeText = minSeconds + " сек";
			}
			Label nextCareTime = new Label(timeText);
			nextCareTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50;");
			nextCareBox.getChildren().addAll(nextCareTitle, nextCareTime);
			card.getChildren().addAll(topButtons, nameLabel, imageView, statusGrid, nextCareBox);
			cardsLayout.getChildren().add(card);
		}
	}

	private void showPlantHistory(PlantState plant) {
		Stage historyStage = new Stage();
		historyStage.setTitle("История ухода: " + plant.getName());
		VBox layout = new VBox(10);
		layout.setPadding(new Insets(15));

		TextArea plantHistory = new TextArea();
		plantHistory.setEditable(false);
		plantHistory.setPrefHeight(400);
		plantHistory.setPrefWidth(500);

		// ИСПРАВЛЕНО: Загружаем индивидуальный лог ухода из БД
		List<String> dbHistory = PlantAndDB.getPlantHistoryFromDB(plant.getId());

		if (dbHistory.isEmpty()) {
			plantHistory.setText("История ухода для растения \"" + plant.getName() + "\" пока пуста.");
		} else {
			StringBuilder sb = new StringBuilder();
			for (String line : dbHistory) {
				sb.append(line).append("\n");
			}
			plantHistory.setText(sb.toString());
		}

		Button closeBtn = new Button("Закрыть");
		closeBtn.setOnAction(e -> historyStage.close());

		layout.getChildren().addAll(plantHistory, closeBtn);
		Scene scene = new Scene(layout, 550, 500);
		historyStage.setScene(scene);
		historyStage.show();
	}

	private void setDefaultImage(ImageView imageView) {
		try {
			Image img = new Image(getClass().getResourceAsStream("/application/images/flower.png"));
			imageView.setImage(img);
		} catch (Exception e) {
			imageView.setStyle("-fx-background-color: #eaeaea; -fx-border-color: #cccccc;");
		}
	}

	private void editPlant(PlantState plant) {
	    Dialog<PlantState> dialog = new Dialog<PlantState>();
	    dialog.setTitle("Редактировать растение");
	    GridPane grid = new GridPane();
	    grid.setHgap(10);
	    grid.setVgap(10);
	    grid.setPadding(new Insets(20));

	    TextField nameField = new TextField(plant.getName());
	    TextField waterField = new TextField(String.valueOf(plant.getWateringIntervalSeconds()));
	    TextField fertilizeField = new TextField(String.valueOf(plant.getFertilizingIntervalSeconds()));
	    TextField repotField = new TextField(String.valueOf(plant.getRepottingIntervalSeconds()));
	    TextField speciesField = new TextField(plant.getSpeciesName()); // Поле текущего вида

	    grid.add(new Label("Название:"), 0, 0);
	    grid.add(nameField, 1, 0);
	    grid.add(new Label("Полив (секунды):"), 0, 1);
	    grid.add(waterField, 1, 1);
	    grid.add(new Label("Удобрение (секунды):"), 0, 2);
	    grid.add(fertilizeField, 1, 2);
	    grid.add(new Label("Пересадка (секунды):"), 0, 3);
	    grid.add(repotField, 1, 3);
	    grid.add(new Label("Вид растения:"), 0, 4);
	    grid.add(speciesField, 1, 4);

	    dialog.getDialogPane().setContent(grid);
	    ButtonType okBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
	    dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

	    dialog.setResultConverter(btn -> {
	        if (btn == okBtn) {
	            try {
	                String inputSpecies = speciesField.getText().trim();
	                
	                // ВАЛИДАЦИЯ ТЗ: Если вид изменен, проверяем, существует ли он в СУБД
	                if (!inputSpecies.equalsIgnoreCase(plant.getSpeciesName())) {
	                    int speciesId = PlantAndDB.getSpeciesIdByName(inputSpecies);
	                    if (speciesId == -1) {
	                        // Вида нет — выкидываем ошибку и прерываем операцию
	                        Platform.runLater(() -> showError("Ошибка! Нельзя назначить вид '" + inputSpecies + "', так как его нет в базе данных!\nДоступные виды: " + PlantAndDB.getAllSpeciesNames()));
	                        return null;
	                    }
	                    // Вид найден — перепривязываем внешние ключи
	                    plant.setPlantSpeciesId(speciesId);
	                    plant.setSpeciesName(inputSpecies);
	                    PlantAndDB.updatePlantSpecies(plant.getId(), speciesId);
	                }

	                plant.setName(nameField.getText());
	                plant.setWateringIntervalSeconds(Integer.parseInt(waterField.getText()));
	                plant.setFertilizingIntervalSeconds(Integer.parseInt(fertilizeField.getText()));
	                plant.setRepottingIntervalSeconds(Integer.parseInt(repotField.getText()));
	                return plant;
	            } catch (NumberFormatException ex) {
	                Platform.runLater(() -> showError("Введите корректные числа!"));
	                return null;
	            }
	        }
	        return null;
	    });

	    dialog.showAndWait().ifPresent(p -> {
	    	PlantAndDB.updatePlantData(p);
	        refreshPlantCards();
	    });
	}


	private void showAddSpeciesDialog() {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.setTitle("Создать новый вид растений");
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20));

		TextField nameField = new TextField();
		TextArea adviceField = new TextArea();
		adviceField.setPrefHeight(100);
		adviceField.setWrapText(true);

		grid.add(new Label("Название вида:"), 0, 0);
		grid.add(nameField, 1, 0);
		grid.add(new Label("Заметка/Советы:"), 0, 1);
		grid.add(adviceField, 1, 1);

		dialog.getDialogPane().setContent(grid);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		dialog.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				String name = nameField.getText().trim();
				String advice = adviceField.getText().trim();

				if (name.isEmpty()) {
					showError("Название вида не может быть пустым!");
					return;
				}

				// Вызываем метод БД с проверкой уникальности
				boolean isCreated = PlantAndDB.insertNewSpecies(name, advice);
				if (isCreated) {
					Alert alert = new Alert(Alert.AlertType.INFORMATION, "Вид '" + name + "' успешно добавлен в БД!",
							ButtonType.OK);
					alert.showAndWait();
				} else {
					showError("Ошибка! Вид растений с названием '" + name + "' уже существует в базе данных!");
				}
			}
		});
	}

	private void showAddPlantDialog() {
		Dialog<PlantState> dialog = new Dialog<PlantState>();
		dialog.setTitle("Новое растение");
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20));

		TextField nameField = new TextField();
		TextField waterField = new TextField();
		TextField fertilizeField = new TextField();
		TextField repotField = new TextField();
		TextField speciesField = new TextField(); // Текстовое поле для ввода вида

		grid.add(new Label("Название:"), 0, 0);
		grid.add(nameField, 1, 0);
		grid.add(new Label("Вид растения (из БД):"), 0, 1);
		grid.add(speciesField, 1, 1);
		grid.add(new Label("Полив (секунды):"), 0, 2);
		grid.add(waterField, 1, 2);
		grid.add(new Label("Удобрение (секунды):"), 0, 3);
		grid.add(fertilizeField, 1, 3);
		grid.add(new Label("Пересадка (секунды):"), 0, 4);
		grid.add(repotField, 1, 4);

		dialog.getDialogPane().setContent(grid);
		ButtonType okBtn = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

		dialog.setResultConverter(btn -> {
			if (btn == okBtn) {
				try {
					String name = nameField.getText().trim();
					String speciesName = speciesField.getText().trim();

					// ВАЛИДАЦИЯ ТЗ: Ищем вид в справочнике
					int speciesId = PlantAndDB.getSpeciesIdByName(speciesName);
					if (speciesId == -1) {
						Platform.runLater(() -> showError("Ошибка! Вида '" + speciesName
								+ "' не существует.\nДоступные виды: " + PlantAndDB.getAllSpeciesNames()));
						return null;
					}

					int water = Integer.parseInt(waterField.getText());
					int fertilize = Integer.parseInt(fertilizeField.getText());
					int repot = Integer.parseInt(repotField.getText());

					PlantState newPlant = new PlantState(0, name, water, fertilize, repot);
					newPlant.setPlantSpeciesId(speciesId);
					newPlant.setSpeciesName(speciesName);
					return newPlant;
				} catch (NumberFormatException ex) {
					Platform.runLater(() -> showError("Введите корректные числа для интервалов!"));
					return null;
				}
			}
			return null;
		});

		dialog.showAndWait().ifPresent(plant -> {
		    // 1. Отправляем новое растение в БД через JDBC и получаем его реальный ID
		    int realDbId = PlantAndDB.insertNewPlant(plant);
		    
		    if (realDbId != -1) {
		        // 2. Присваиваем объекту в Java реальный ID, сгенерированный СУБД
		        plant.setId(realDbId);
		        
		        // 3. Добавляем в список и перерисовываем интерфейс
		        plants.add(plant);
		        refreshPlantCards();
		        
		        System.out.println("🌱 Растение \"" + plant.getName() + "\" успешно создано в БД с ID: " + realDbId);
		    } else {
		        showError("Не удалось сохранить растение в базу данных بسبب ошибки SQL-транзакции.");
		    }
		});

	}

	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
		alert.showAndWait();
	}
}