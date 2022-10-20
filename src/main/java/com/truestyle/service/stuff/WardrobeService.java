package com.truestyle.service.stuff;

import com.truestyle.entity.stuff.ShopStuff;
import com.truestyle.entity.stuff.UserStuff;
import com.truestyle.entity.user.User;
import com.truestyle.pojo.WardrobeResponse;
import com.truestyle.repository.stuff.StuffShopRepository;
import com.truestyle.repository.stuff.StuffUserRepository;
import com.truestyle.repository.user.UserRepository;
import com.truestyle.service.user.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// -------------------------------------Проверить и дописать--------------------------------//

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WardrobeService {

    private final UserRepository userRepository;

    private final StuffShopRepository shopStuffRepository;

    private final StuffUserRepository userStuffRepository;

    private final SecurityService auth;

    @Value("${upload.path.user}")
    private String uploadPathUser;

    @Value("${upload.path.shop}")
    private String uploadPathShop;

    private List<String> uniqueArtType;

    // Получить информацию об одной вещи из гардероба(МАГАЗИН)
    public UserStuff getUserStuff(Long stuffId){
        User user = auth.getAuthUser();
        return userStuffRepository.findById(stuffId).orElseThrow(() -> new RuntimeException("Error, Stuff is not found!"));
    }

    // Получить информацию об одной вещи из гардероба(пользователь)
    public ByteArrayResource getImage(String type, String stuffUrl) throws IOException {
        User user = auth.getAuthUser();
        String stuffPath = "";

        if (type.equals("shop")){
            stuffPath = uploadPathShop + shopStuffRepository.findByImageUrl("/" + stuffUrl).orElseThrow(() -> new RuntimeException("Error, Stuff is not found!")).getImageUrl();
        } else if (type.equals("user")){
            stuffPath = uploadPathUser + userStuffRepository.findByImageUrl("/" + stuffUrl).orElseThrow(() -> new RuntimeException("Error, Stuff is not found!")).getImageUrl();
        } else {
            // Вызов ошибки
        }
        File file = new File(stuffPath);
        return new ByteArrayResource(Files.readAllBytes(file.toPath()));
    }

    // Получить информацию об одной вещи из гардероба(пользователь)
    public ShopStuff getShopStuff(Long stuffId){
        User user = auth.getAuthUser();
        return shopStuffRepository.findById(stuffId).orElseThrow(() -> new RuntimeException("Error, Stuff is not found!"));
    }

    // Получить все шмотки пользователя (хз, зачем), треню малехо)
    public WardrobeResponse getWardrobe(){
        User user = auth.getAuthUser();

        // Создаем гардероб
        WardrobeResponse wardrobe = new WardrobeResponse();

        wardrobe.setShopsStuffs(new ArrayList<ShopStuff>(user.getWardrobeShops()));
        wardrobe.setUsersStuffs(new ArrayList<UserStuff>(user.getWardrobeUsers()));
        return wardrobe;
    }

    // Выдать шмотки пользователю по определенному сезону
    public WardrobeResponse getWardrobeBySeason(String season){
        User user = auth.getAuthUser();

        ArrayList<ShopStuff> shopsStuff = new ArrayList<>();
        user.getWardrobeShops().forEach(stuff -> {
            if (stuff.getSeason().equalsIgnoreCase(season)){
                shopsStuff.add(stuff);
            }
        });

        ArrayList<UserStuff> usersStuff = new ArrayList<>();
        user.getWardrobeUsers().forEach(stuff -> {
            if (stuff.getSeason().equalsIgnoreCase(season)){
                usersStuff.add(stuff);
            }
        });

        // Создаем гардероб
        WardrobeResponse wardrobe = new WardrobeResponse();

        wardrobe.setShopsStuffs(shopsStuff);
        wardrobe.setUsersStuffs(usersStuff);
        return wardrobe;
    }

    // Проверка наличия вещи в гардеробе
    public Boolean checkStuffInWardrobe(String typeWardrobe, Long stuffId){
        User user = auth.getAuthUser();

        if (typeWardrobe.equalsIgnoreCase("shop")){
            return userRepository.existsStuffInShopsWardrobe(user.getId(), stuffId);
        }
        return userRepository.existsStuffInUsersWardrobe(user.getId(), stuffId);
    }

    // Проверить, есть ли ArticleType пользователя в базе данных
    public Boolean existArticleTypeInDB(String articleType){
        // Проверка соответствия типа с
        if (uniqueArtType == null) uniqueArtType = shopStuffRepository.findArticleTypes();

        // Проверка для Александры 😘
        if (uniqueArtType.contains(articleType)) return true;
        return false;
    }

    public List<String> findAllArticleTypeInShops(){
        return shopStuffRepository.findArticleTypes();
    }

    // Добавить шмотку пользователю
    public String addUsersStuffInWardrobe(UserStuff stuffInfo, MultipartFile file) throws IOException {

        User user = auth.getAuthUser();

        if (userStuffRepository.countUserStuff(user.getId()) > 110){
            return "The wardrobe is limited to 110 items!";
        }

        // Проверка корректности articleType
        if (!existArticleTypeInDB(stuffInfo.getArticleType())){
            return "ArticleType isn't found";
        }

        String result = "";

        // Проверка на наличие файла
        if (!file.isEmpty()) {
            File uploadDir = new File(uploadPathUser);

            // Создадим директорию, если ее нет
            if (!uploadDir.exists()) {
                result = uploadDir.mkdir()? "Good": "Bad";
            }

            // Сохраним уникальное имя файла
            String uuidFile = UUID.randomUUID().toString();
            String resultFilename = uuidFile + "." + file.getOriginalFilename();
            String resultPath = uploadDir.getAbsolutePath() + "/" + resultFilename;

            // Сохраняем месторасположение файла на сервере
            stuffInfo.setImageUrl(resultFilename);

            // Сохраним файл
            file.transferTo(new File(resultPath));

            stuffInfo.setImageUrl("/" + resultFilename);

            // Удалить файл, если не получилось сохранить запись в бд
            Path path = Paths.get(resultPath);
            try{
                // Сохраняем шмотку в бд
                userStuffRepository.save(stuffInfo);
            } catch (RuntimeException e){
                Files.delete(path);
            }

            try{
                // Сохраняем шмотку у пользователя
                if (user.addUsersStuff(stuffInfo)) {
                    result = "Good";
                    userRepository.save(user);
                }
            } catch (RuntimeException e){
                userStuffRepository.delete(stuffInfo);
                Files.delete(path);
            }
        }

        return result;
    }

    // Добавить магазинную шмотку пользователю
    public Boolean addShopsStuffInWardrobe(Long stuffId){

        User user = auth.getAuthUser();

        ShopStuff shopsStuff = shopStuffRepository.findById(stuffId).orElseThrow(() -> new RuntimeException("Error, Stuff is not found!"));
        Boolean result = user.addShopsStuff(shopsStuff);
        userRepository.save(user);

        return result;
    }

    // Забрать шмотку у пользователя и погладить
    public Boolean deleteUsersStuffInWardrobe(Long stuffId) throws IOException {

        User user = auth.getAuthUser();

        Boolean result = false;

        UserStuff userStuff = userStuffRepository.findById(stuffId).orElseThrow(() -> new RuntimeException("Error, Stuff is not found!"));


        // ------------------- Код удаления файла ---------------
        result = user.deleteUsersStuff(userStuff);
        userRepository.save(user);

        File dirImage = new File(uploadPathUser);

        // Удалить файл
        Files.delete(Paths.get(dirImage.getAbsolutePath() + userStuff.getImageUrl()));
        userStuffRepository.delete(userStuff);

        return result;
    }

    // Забрать шмотку у пользователя(магазинную), но не бить)
    public Boolean deleteShopsStuffInWardrobe(Long stuffId) {

        User user = auth.getAuthUser();

        ShopStuff shopsStuff = shopStuffRepository.findById(stuffId).orElseThrow(() -> new RuntimeException("Error, Stuff is not found!"));
        Boolean result = user.deleteShopsStuff(shopsStuff);
        userRepository.save(user);

        return result;
    }


    // Отметить шмотку как понравившуюся
    public void likeStuff(ShopStuff shopsStuff){
        User user = auth.getAuthUser();
        user.likeShopsStuff(shopsStuff);
        userRepository.save(user);
    }

    // Убрать лайк со шмотки
    public void dislikeStuff(ShopStuff shopsStuff){
        User user = auth.getAuthUser();
        user.dislikeShopsStuff(shopsStuff);
        userRepository.save(user);
    }

}
