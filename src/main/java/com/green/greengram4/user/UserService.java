package com.green.greengram4.user;

import com.green.greengram4.common.*;
import com.green.greengram4.entity.UserEntity;
import com.green.greengram4.entity.UserFollowEntity;
import com.green.greengram4.entity.UserFollowIds;
import com.green.greengram4.exception.AuthErrorCode;
import com.green.greengram4.exception.RestApiException;
import com.green.greengram4.security.AuthenticationFacade;
import com.green.greengram4.security.JwtTokenProvider;
import com.green.greengram4.security.MyPrincipal;
import com.green.greengram4.security.MyUserDetails;
import com.green.greengram4.user.model.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper mapper;
    private final UserRepository repository;
    private final UserFollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;
    private final CookieUtils cookieUtils;
    private final AuthenticationFacade authenticationFacade;
    private final MyFileUtils myFileUtils;

    public ResVo signup(UserSignupDto dto) {
        String hashedPw = passwordEncoder.encode(dto.getUpw());
        UserEntity entity = UserEntity.builder()
                .providerType(ProviderTypeEnum.LOCAL)
                .uid(dto.getUid())
                .upw(hashedPw)
                .nm(dto.getNm())
                .pic(dto.getPic())
                .role(RoleEnum.USER)
                .build();
        UserEntity result = repository.save(entity);
        return new ResVo(result.getIuser().intValue());
    }



//    public ResVo signup(UserSignupDto dto) {
//        String hashedPw = passwordEncoder.encode(dto.getUpw());
//        //비밀번호 암호화
//
//        UserSignupProcDto pDto = new UserSignupProcDto();
//        pDto.setUid(dto.getUid());
//        pDto.setUpw(hashedPw);
//        pDto.setNm(dto.getNm());
//        pDto.setPic(dto.getPic());
//
//        log.info("before - pDto.iuser : {}", pDto.getIuser());
//        int affectedRows = mapper.insUser(pDto);
//        log.info("after - pDto.iuser : {}", pDto.getIuser());
//
//        return new ResVo(pDto.getIuser()); //회원가입한 iuser pk값이 리턴
//    }

    public UserSigninVo signin(HttpServletResponse res, UserSigninDto dto) {
        Optional<UserEntity> optEntity = repository.findByProviderTypeAndUid(ProviderTypeEnum.LOCAL, dto.getUid());
        UserEntity entity = optEntity.orElseThrow(() -> new RestApiException(AuthErrorCode.NOT_EXIST_USER_ID));

        if(!passwordEncoder.matches(dto.getUpw(), entity.getUpw())) {
            throw new RestApiException(AuthErrorCode.VALID_PASSWORD);
        }

        int iuser = entity.getIuser().intValue();
        MyPrincipal myPrincipal = MyPrincipal.builder()
                .iuser(iuser)
                .build();
        myPrincipal.getRoles().add(entity.getRole().name());

        String at = jwtTokenProvider.generateAccessToken(myPrincipal);
        String rt = jwtTokenProvider.generateRefreshToken(myPrincipal);

        //rt > cookie에 담을꺼임
        int rtCookieMaxAge = appProperties.getJwt().getRefreshTokenCookieMaxAge();
        cookieUtils.deleteCookie(res, "rt");
        cookieUtils.setCookie(res, "rt", rt, rtCookieMaxAge);

        return UserSigninVo.builder()
                .result(Const.SUCCESS)
                .iuser(iuser)
                .nm(entity.getNm())
                .pic(entity.getPic())
                .firebaseToken(entity.getFirebaseToken())
                .accessToken(at)
                .build();

    }
    /*
    public UserSigninVo signin(HttpServletResponse res, UserSigninDto dto) {
        UserSelDto sDto = new UserSelDto();
        sDto.setUid(dto.getUid());

        UserModel entity = mapper.selUser(sDto);
        if(entity == null) { //아이디 없음
            throw new RestApiException(AuthErrorCode.NOT_EXIST_USER_ID);
        } else if(!passwordEncoder.matches(dto.getUpw(), entity.getUpw())) { // 비밀번호를 확인해 주세요.
            //return UserSigninVo.builder().result(Const.LOGIN_DIFF_UPW).build();
            throw new RestApiException(AuthErrorCode.VALID_PASSWORD);
        }

        MyPrincipal myPrincipal = MyPrincipal.builder()
                                            .iuser(entity.getIuser())
                                            .build();
        myPrincipal.getRoles().add(entity.getRole());


        String at = jwtTokenProvider.generateAccessToken(myPrincipal);
        String rt = jwtTokenProvider.generateRefreshToken(myPrincipal);

        //rt > cookie에 담을꺼임
        int rtCookieMaxAge = appProperties.getJwt().getRefreshTokenCookieMaxAge();
        cookieUtils.deleteCookie(res, "rt");
        cookieUtils.setCookie(res, "rt", rt, rtCookieMaxAge);

        return UserSigninVo.builder()
                .result(Const.SUCCESS)
                .iuser(entity.getIuser())
                .nm(entity.getNm())
                .pic(entity.getPic())
                .firebaseToken(entity.getFirebaseToken())
                .accessToken(at)
                .build();
    }
*/
    public ResVo signout(HttpServletResponse res) {
        cookieUtils.deleteCookie(res, "rt");
        return new ResVo(1);
    }

    public UserSigninVo getRefreshToken(HttpServletRequest req) {
        //Cookie cookie = cookieUtils.getCookie(req, "rt");
        Optional<String> optRt = cookieUtils.getCookie(req, "rt").map(Cookie::getValue);
        if(optRt.isEmpty()) {
            return UserSigninVo.builder()
                    .result(Const.FAIL)
                    .accessToken(null)
                    .build();
        }
        String token = optRt.get();
        if(!jwtTokenProvider.isValidateToken(token)) {
            return UserSigninVo.builder()
                    .result(Const.FAIL)
                    .accessToken(null)
                    .build();
        }
        MyUserDetails myUserDetails = (MyUserDetails) jwtTokenProvider.getUserDetailsFromToken(token);
        MyPrincipal myPrincipal = myUserDetails.getMyPrincipal();

        String at = jwtTokenProvider.generateAccessToken(myPrincipal);

        return UserSigninVo.builder()
                .result(Const.SUCCESS)
                .accessToken(at)
                .build();
    }

    public UserInfoVo getUserInfo(UserInfoSelDto dto) {
        return mapper.selUserInfo(dto);
    }

    @Transactional
    public ResVo patchUserFirebaseToken(UserFirebaseTokenPatchDto dto) {
        UserEntity entity = repository.getReferenceById((long)authenticationFacade.getLoginUserPk());
        entity.setFirebaseToken(dto.getFirebaseToken());
        return new ResVo(Const.SUCCESS);
    }
    /*
        public ResVo patchUserFirebaseToken(UserFirebaseTokenPatchDto dto) {
        int affectedRows = mapper.updUserFirebaseToken(dto);
        return new ResVo(affectedRows);
    }
    */

    @Transactional
    public UserPicPatchDto patchUserPic(MultipartFile pic) {
        Long iuser = Long.valueOf(authenticationFacade.getLoginUserPk());
        UserEntity entity = repository.getReferenceById(iuser);
        String path = "/user/" + iuser;
        myFileUtils.delFolderTrigger(path);
        String savedPicFileNm = myFileUtils.transferTo(pic, path);
        entity.setPic(savedPicFileNm);

        UserPicPatchDto dto = new UserPicPatchDto();
        dto.setIuser(iuser.intValue());
        dto.setPic(savedPicFileNm);
        return dto;
    }

//    public UserPicPatchDto patchUserPic(MultipartFile pic) {
//        UserPicPatchDto dto = new UserPicPatchDto();
//        dto.setIuser(authenticationFacade.getLoginUserPk());
//        String path = "/user/" + dto.getIuser();
//        myFileUtils.delFolderTrigger(path);
//        String savedPicFileNm = myFileUtils.transferTo(pic, path);
//        dto.setPic(savedPicFileNm);
//        int affectedRows = mapper.updUserPic(dto);
//        return dto;
//    }


    public ResVo toggleFollow(UserFollowDto dto) {
        UserFollowIds ids = new UserFollowIds();
        ids.setFromIuser((long)authenticationFacade.getLoginUserPk());
        ids.setToIuser(dto.getToIuser());

        AtomicInteger atomic = new AtomicInteger(Const.FAIL);
        followRepository
        .findById(ids)
        .ifPresentOrElse(
            entity -> followRepository.delete(entity)
            , () -> {
                atomic.set(Const.SUCCESS);
                UserFollowEntity saveUserFollowEntity = new UserFollowEntity();
                saveUserFollowEntity.setUserFollowIds(ids);
                UserEntity fromUserEntity = repository.getReferenceById((long)authenticationFacade.getLoginUserPk());
                UserEntity toUserEntity = repository.getReferenceById(dto.getToIuser());
                saveUserFollowEntity.setFromUserEntity(fromUserEntity);
                saveUserFollowEntity.setToUserEntity(toUserEntity);
                followRepository.save(saveUserFollowEntity);
            }
        );
        return new ResVo(atomic.get());
    }

    /*
    public ResVo toggleFollow(UserFollowDto dto) {
        int delAffectedRows = mapper.delUserFollow(dto);
        if(delAffectedRows == 1) {
            return new ResVo(Const.FAIL);
        }
        int insAffectedRows = mapper.insUserFollow(dto);
        return new ResVo(Const.SUCCESS);
    }
     */
}
